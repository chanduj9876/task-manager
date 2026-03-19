package com.taskmanager.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private String entityType;
    private String entityId;
    private String action;
    private UUID performedBy;
    private String performedByName;
    private String details;
    private LocalDateTime timestamp;
}
