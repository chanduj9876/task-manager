package com.taskmanager.task.dto;

import com.taskmanager.task.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
