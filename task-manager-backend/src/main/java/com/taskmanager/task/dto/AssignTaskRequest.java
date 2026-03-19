package com.taskmanager.task.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AssignTaskRequest {

    /** Null means unassign the task. */
    private UUID assignedTo;
}
