package com.taskmanager.notification.strategy;

import com.taskmanager.notification.dto.TaskEventDto;

public interface NotificationStrategy {
    boolean supports(String eventType);
    String buildMessage(TaskEventDto event);
}
