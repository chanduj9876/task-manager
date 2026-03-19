package com.taskmanager.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event payload published to Kafka topic 'task-events'.
 * Observer Pattern: TaskService acts as the publisher (Subject);
 * NotificationConsumer acts as the observer (Subscriber).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEventDto {
    private UUID taskId;
    private String taskTitle;
    private String eventType;   // TASK_CREATED, TASK_UPDATED, TASK_ASSIGNED, STATUS_CHANGED, ORG_INVITATION_SENT, ORG_INVITATION_ACCEPTED
    private UUID actorId;
    private String actorName;
    private UUID orgId;
    private String orgName;     // nullable — for org events
    private UUID assigneeId;    // nullable — target recipient of the notification
    private UUID relatedOrgId;  // nullable — for org invitation events
}
