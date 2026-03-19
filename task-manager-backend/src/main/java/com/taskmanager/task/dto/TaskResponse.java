package com.taskmanager.task.dto;

import com.taskmanager.task.enums.TaskPriority;
import com.taskmanager.task.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TaskResponse {
    private UUID id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private UUID orgId;
    private UUID assignedTo;
    private String assignedToName;
    private UUID createdBy;
    private String createdByName;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
