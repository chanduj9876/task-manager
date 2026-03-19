package com.taskmanager.audit.kafka;

import com.taskmanager.audit.dto.AuditEventDto;
import com.taskmanager.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publishes audit events to Kafka for async processing.
 * Audit log writes are decoupled from business transactions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditProducer {

    private final KafkaTemplate<String, AuditEventDto> auditKafkaTemplate;

    @Async
    public void publishAuditEvent(AuditEventDto event) {
        try {
            auditKafkaTemplate.send(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, event.getEntityId(), event)
                    .thenAccept(result -> log.debug("Published audit event: {}.{} for entity {}",
                            event.getEntityType(), event.getAction(), event.getEntityId()))
                    .exceptionally(ex -> {
                        log.error("Failed to publish audit event for {} {}: {}",
                                event.getEntityType(), event.getEntityId(), ex.getMessage());
                        return null;
                    });
        } catch (Exception ex) {
            log.error("Kafka unavailable, audit event lost for {} {}: {}",
                    event.getEntityType(), event.getEntityId(), ex.getMessage());
        }
    }
}
