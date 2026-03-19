package com.taskmanager.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationDto {
    private UUID id;
    private UUID userId;
    private String message;
    private String eventType;
    private UUID relatedTaskId;
    private UUID relatedOrgId;
    private boolean read;
    private LocalDateTime createdAt;
}
