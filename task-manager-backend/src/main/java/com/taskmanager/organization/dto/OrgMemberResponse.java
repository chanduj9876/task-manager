package com.taskmanager.organization.dto;

import com.taskmanager.organization.entity.InvitationStatus;
import com.taskmanager.user.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrgMemberResponse {
    private UUID userId;
    private String name;
    private String email;
    private Role role;
    private InvitationStatus status;
    private LocalDateTime joinedAt;
}
