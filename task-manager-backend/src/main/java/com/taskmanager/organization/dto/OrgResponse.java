package com.taskmanager.organization.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrgResponse {
    private UUID id;
    private String name;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private int memberCount;
}
