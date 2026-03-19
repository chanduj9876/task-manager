package com.taskmanager.notification.strategy;

import com.taskmanager.notification.dto.TaskEventDto;
import org.springframework.stereotype.Component;

@Component
public class OrgInvitationSentStrategy implements NotificationStrategy {

    @Override
    public boolean supports(String eventType) {
        return "ORG_INVITATION_SENT".equals(eventType);
    }

    @Override
    public String buildMessage(TaskEventDto event) {
        return String.format("%s invited you to join \"%s\". Go to Organizations to respond.",
                event.getActorName(), event.getOrgName());
    }
}
