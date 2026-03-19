package com.taskmanager.auth.dto;

import com.taskmanager.user.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private UUID userId;
    private String name;
    private String email;
    private Role role;
}
