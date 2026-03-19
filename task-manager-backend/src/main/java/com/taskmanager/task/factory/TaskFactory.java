package com.taskmanager.task.factory;

import com.taskmanager.task.dto.TaskRequest;
import com.taskmanager.task.entity.Task;
import com.taskmanager.task.enums.TaskStatus;

import java.util.UUID;

/**
 * Factory Pattern: centralises Task construction logic,
 * decoupling the service layer from entity instantiation details.
 */
public final class TaskFactory {

    private TaskFactory() {}

    public static Task createTask(TaskRequest request, UUID orgId, UUID creatorId) {
        return Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .priority(request.getPriority())
                .orgId(orgId)
                .assignedTo(request.getAssignedTo())
                .dueDate(request.getDueDate())
                .createdBy(creatorId)
                .build();
    }
}
