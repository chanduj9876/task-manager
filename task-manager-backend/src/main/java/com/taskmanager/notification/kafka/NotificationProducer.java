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
        try {
            kafkaTemplate.send(KafkaConfig.TASK_EVENTS_TOPIC, event.getTaskId().toString(), event)
                    .thenAccept(result -> log.debug("Published task event: {} for task {}",
                            event.getEventType(), event.getTaskId()))
                    .exceptionally(ex -> {
                        log.warn("Failed to publish task event for task {}: {}",
                                event.getTaskId(), ex.getMessage());
                        return null;
                    });
        } catch (Exception ex) {
            log.warn("Kafka unavailable, skipping event for task {}: {}",
                    event.getTaskId(), ex.getMessage());
        }
    }
}
