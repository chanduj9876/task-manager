package com.taskmanager.notification.strategy;

import com.taskmanager.notification.dto.TaskEventDto;
import org.springframework.stereotype.Component;

@Component
public class OrgInvitationAcceptedStrategy implements NotificationStrategy {

    @Override
    public boolean supports(String eventType) {
        return "ORG_INVITATION_ACCEPTED".equals(eventType);
    }

    @Override
    public String buildMessage(TaskEventDto event) {
        return String.format("%s accepted your invitation to join \"%s\".",
                event.getActorName(), event.getOrgName());
    }
}
