package com.taskmanager.notification.kafka;

import com.taskmanager.config.KafkaConfig;
import com.taskmanager.notification.dto.TaskEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern (Publisher side):
 * Publishes task events to Kafka for async processing by NotificationConsumer.
 */
@Component
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, TaskEventDto> kafkaTemplate;

    public NotificationProducer(@Qualifier("taskEventsKafkaTemplate") KafkaTemplate<String, TaskEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    public void publishTaskEvent(TaskEventDto event) {
        // For org events taskId is null — fall back to orgId as the partition key
        String partitionKey = event.getTaskId() != null
                ? event.getTaskId().toString()
                : (event.getOrgId() != null ? event.getOrgId().toString() : "unknown");
        try {
            kafkaTemplate.send(KafkaConfig.TASK_EVENTS_TOPIC, partitionKey, event)
                    .thenAccept(result -> log.debug("Published task event: {} key={}",
                            event.getEventType(), partitionKey))
                    .exceptionally(ex -> {
                        log.warn("Failed to publish task event type={} key={}: {}",
                                event.getEventType(), partitionKey, ex.getMessage());
                        return null;
                    });
        } catch (Exception ex) {
            log.warn("Kafka unavailable, skipping event type={} key={}: {}",
                    event.getEventType(), partitionKey, ex.getMessage());
        }
    }
}
