package com.taskmanager.notification.strategy;

import com.taskmanager.notification.dto.TaskEventDto;
import org.springframework.stereotype.Component;

@Component
public class TaskAssignedStrategy implements NotificationStrategy {

    @Override
    public boolean supports(String eventType) {
        return "TASK_ASSIGNED".equals(eventType);
    }

    @Override
    public String buildMessage(TaskEventDto event) {
        return String.format("You have been assigned to task: \"%s\"", event.getTaskTitle());
    }
}
