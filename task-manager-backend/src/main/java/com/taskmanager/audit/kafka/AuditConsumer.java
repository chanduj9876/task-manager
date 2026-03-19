package com.taskmanager.audit.kafka;

import com.taskmanager.audit.dto.AuditEventDto;
import com.taskmanager.audit.entity.AuditLog;
import com.taskmanager.audit.repository.AuditLogRepository;
import com.taskmanager.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes audit events from Kafka and persists them to the database asynchronously.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditConsumer {

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(
            topics = KafkaConfig.NOTIFICATION_EVENTS_TOPIC,
            groupId = "audit-consumer-group",
            containerFactory = "auditKafkaListenerContainerFactory"
    )
    public void handleAuditEvent(AuditEventDto event) {
        try {
            log.debug("Processing audit event: {}.{} for entity {}",
                    event.getEntityType(), event.getAction(), event.getEntityId());

            AuditLog auditLog = AuditLog.builder()
                    .entityType(event.getEntityType())
                    .entityId(event.getEntityId())
                    .action(event.getAction())
                    .performedBy(event.getPerformedBy())
                    .performedByName(event.getPerformedByName())
                    .details(event.getDetails())
                    .oldValue(event.getOldValue())
                    .newValue(event.getNewValue())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log persisted: {}.{}", event.getEntityType(), event.getAction());
        } catch (Exception e) {
            log.error("Failed to persist audit event for {} {}: {}",
                    event.getEntityType(), event.getEntityId(), e.getMessage(), e);
            throw e; // Re-throw to trigger DLQ
        }
    }
}
