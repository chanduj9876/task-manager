package com.taskmanager.notification.strategy;

import com.taskmanager.notification.dto.TaskEventDto;
import org.springframework.stereotype.Component;

@Component
public class StatusChangedStrategy implements NotificationStrategy {

    @Override
    public boolean supports(String eventType) {
        return "STATUS_CHANGED".equals(eventType);
    }

    @Override
    public String buildMessage(TaskEventDto event) {
        return String.format("Task \"%s\" status was changed by %s",
                event.getTaskTitle(), event.getActorName());
    }
}
