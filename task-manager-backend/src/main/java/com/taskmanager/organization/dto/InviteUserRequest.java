package com.taskmanager.organization.dto;

import com.taskmanager.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /** When true the user is added immediately (force-add). When false an invitation is sent that the user must accept. */
    private boolean force = false;

    /** Role to assign to the invited user. Defaults to MEMBER. */
    private Role role = Role.MEMBER;
}
