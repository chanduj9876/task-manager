package com.taskmanager.audit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.audit.annotation.Auditable;
import com.taskmanager.audit.dto.AuditEventDto;
import com.taskmanager.audit.kafka.AuditProducer;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditProducer auditProducer;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        UUID performedBy = extractCurrentUserId();

        // Capture old state BEFORE the method mutates the entity
        String oldValue = null;
        if (auditable.entityClass() != Void.class && !"CREATE".equals(auditable.action())) {
            oldValue = captureOldValue(joinPoint, auditable.entityClass());
        }

        Object result = joinPoint.proceed();

        try {
            // For CREATE actions, extract the entity ID from the result (not args).
            // For other actions, the first arg is the entity's UUID.
            String entityId = "CREATE".equals(auditable.action())
                    ? extractEntityIdFromResult(result)
                    : extractEntityId(joinPoint);

            String performedByName = null;
            if (performedBy != null) {
                performedByName = userRepository.findById(performedBy)
                        .map(User::getName).orElse(null);
            }

            String details = buildDetails(result, auditable.action());

            AuditEventDto auditEvent = AuditEventDto.builder()
                    .entityType(auditable.entityType())
                    .entityId(entityId)
                    .action(auditable.action())
                    .performedBy(performedBy)
                    .performedByName(performedByName)
                    .details(details)
                    .oldValue(oldValue)
                    .newValue(result != null ? objectMapper.writeValueAsString(result) : null)
                    .build();

            // Publish to Kafka for async persistence — audit write is decoupled from business transaction
            auditProducer.publishAuditEvent(auditEvent);
        } catch (Exception e) {
            log.warn("Failed to publish audit event for {}.{}: {}",
                    auditable.entityType(), auditable.action(), e.getMessage());
        }

        return result;
    }

    private String captureOldValue(ProceedingJoinPoint joinPoint, Class<?> entityClass) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0 && args[0] instanceof UUID id) {
                Object entity = entityManager.find(entityClass, id);
                if (entity != null) {
                    return objectMapper.writeValueAsString(entity);
                }
            }
        } catch (Exception e) {
            log.warn("Could not capture oldValue for {}: {}", entityClass.getSimpleName(), e.getMessage());
        }
        return null;
    }

    private String buildDetails(Object result, String action) {
        if (result == null) return null;
        try {
            // Extract a human-readable summary from the result object using reflection
            var titleMethod = findGetter(result, "title", "name", "email");
            if (titleMethod != null) {
                return action + ": " + titleMethod.invoke(result);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private java.lang.reflect.Method findGetter(Object obj, String... fieldNames) {
        for (String field : fieldNames) {
            String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            try {
                return obj.getClass().getMethod(getter);
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private UUID extractCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getId();
        }
        return null;
    }

    private String extractEntityId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof UUID id) {
            return id.toString();
        }
        return "unknown";
    }

    private String extractEntityIdFromResult(Object result) {
        if (result == null) return "unknown";
        try {
            java.lang.reflect.Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            return id != null ? id.toString() : "unknown";
        } catch (Exception ignored) {}
        return "unknown";
    }
}

