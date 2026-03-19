package com.taskmanager.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDto {
    private String entityType;
    private String entityId;
    private String action;
    private UUID performedBy;
    private String performedByName;
    private String details;
    private String oldValue;
    private String newValue;
}
