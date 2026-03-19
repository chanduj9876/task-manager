package com.taskmanager.task.service;

import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.task.dto.*;
import com.taskmanager.task.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ITaskService {
    TaskResponse createTask(UUID orgId, TaskRequest request, UserDetailsImpl currentUser);
    TaskResponse updateTask(UUID taskId, TaskRequest request, UserDetailsImpl currentUser);
    TaskResponse assignTask(UUID taskId, AssignTaskRequest request, UserDetailsImpl currentUser);
    TaskResponse changeStatus(UUID taskId, UpdateStatusRequest request, UserDetailsImpl currentUser);
    void deleteTask(UUID taskId, UserDetailsImpl currentUser);
    TaskResponse getTask(UUID taskId, UserDetailsImpl currentUser);
    Page<TaskResponse> getTasks(UUID orgId, TaskStatus status, UUID assignedTo, Pageable pageable, UserDetailsImpl currentUser);
}
