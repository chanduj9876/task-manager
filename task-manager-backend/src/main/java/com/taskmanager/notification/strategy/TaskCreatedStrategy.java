package com.taskmanager.notification.strategy;

import com.taskmanager.notification.dto.TaskEventDto;
import org.springframework.stereotype.Component;

@Component
public class TaskCreatedStrategy implements NotificationStrategy {

    @Override
    public boolean supports(String eventType) {
        return "TASK_CREATED".equals(eventType);
    }

    @Override
    public String buildMessage(TaskEventDto event) {
        return String.format("You have been assigned to new task: \"%s\"", event.getTaskTitle());
    }
}
