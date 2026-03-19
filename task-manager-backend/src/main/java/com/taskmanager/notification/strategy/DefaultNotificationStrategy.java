package com.taskmanager.notification.strategy;

import com.taskmanager.notification.dto.TaskEventDto;

/**
 * Fallback strategy used when no specific strategy matches the event type.
 * Not a Spring bean — instantiated directly as a constant in NotificationConsumer.
 */
public class DefaultNotificationStrategy implements NotificationStrategy {

    @Override
    public boolean supports(String eventType) {
        return true;
    }

    @Override
    public String buildMessage(TaskEventDto event) {
        return String.format("Update on task: \"%s\"", event.getTaskTitle());
    }
}
