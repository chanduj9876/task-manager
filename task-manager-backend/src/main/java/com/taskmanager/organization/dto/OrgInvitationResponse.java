package com.taskmanager.organization.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrgInvitationResponse {
    private UUID orgId;
    private String orgName;
    private LocalDateTime invitedAt;
}
