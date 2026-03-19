package com.taskmanager.notification.strategy;

import com.taskmanager.notification.dto.TaskEventDto;
import org.springframework.stereotype.Component;

@Component
public class TaskUpdatedStrategy implements NotificationStrategy {

    @Override
    public boolean supports(String eventType) {
        return "TASK_UPDATED".equals(eventType);
    }

    @Override
    public String buildMessage(TaskEventDto event) {
        return String.format("Task \"%s\" has been updated", event.getTaskTitle());
    }
}
