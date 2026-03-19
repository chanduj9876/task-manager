package com.taskmanager.notification.kafka;

import com.taskmanager.config.KafkaConfig;
import com.taskmanager.notification.dto.NotificationDto;
import com.taskmanager.notification.dto.TaskEventDto;
import com.taskmanager.notification.entity.Notification;
import com.taskmanager.notification.repository.NotificationRepository;
import com.taskmanager.notification.strategy.DefaultNotificationStrategy;
import com.taskmanager.notification.strategy.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Observer Pattern (Subscriber side):
 * Consumes task events from Kafka, persists Notification entities,
 * and pushes real-time updates to connected WebSocket clients.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final List<NotificationStrategy> strategies;

    private static final NotificationStrategy DEFAULT = new DefaultNotificationStrategy();

    @KafkaListener(topics = KafkaConfig.TASK_EVENTS_TOPIC,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleTaskEvent(TaskEventDto event) {
        log.debug("Received task event: {} for task {}", event.getEventType(), event.getTaskId());

        // Notify the assignee for all event types that affect them,
        // but skip self-notification (e.g. the assignee changes their own task's status).
        if (event.getAssigneeId() != null) {
            boolean actorIsAssignee = event.getAssigneeId().equals(event.getActorId());
            boolean skipSelf = actorIsAssignee && "STATUS_CHANGED".equals(event.getEventType());
            if (!skipSelf) {
                Notification notification = buildNotification(event, event.getAssigneeId());
                notificationRepository.save(notification);
                pushToWebSocket(notification);
            }
        }
    }

    private Notification buildNotification(TaskEventDto event, java.util.UUID targetUserId) {
        String message = strategies.stream()
                .filter(s -> s.supports(event.getEventType()))
                .findFirst()
                .orElse(DEFAULT)
                .buildMessage(event);

        return Notification.builder()
                .userId(targetUserId)
                .message(message)
                .eventType(event.getEventType())
                .relatedTaskId(event.getTaskId())
                .relatedOrgId(event.getRelatedOrgId())
                .read(false)
                .build();
    }

    private void pushToWebSocket(Notification notification) {
        try {
            NotificationDto dto = NotificationDto.builder()
                    .id(notification.getId())
                    .userId(notification.getUserId())
                    .message(notification.getMessage())
                    .eventType(notification.getEventType())
                    .relatedTaskId(notification.getRelatedTaskId())
                    .relatedOrgId(notification.getRelatedOrgId())
                    .read(notification.isRead())
                    .createdAt(notification.getCreatedAt())
                    .build();
            messagingTemplate.convertAndSendToUser(
                    notification.getUserId().toString(),
                    "/queue/notifications",
                    dto);
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {}: {}",
                    notification.getUserId(), e.getMessage());
        }
    }
}
