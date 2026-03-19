package com.taskmanager.task.controller;

import com.taskmanager.common.response.ApiResponse;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.task.dto.*;
import com.taskmanager.task.enums.TaskStatus;
import com.taskmanager.task.service.ITaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tasks", description = "Create, read, update, delete and manage task status/assignment")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;

    @Operation(summary = "Create a task in an organization")
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @RequestParam UUID orgId,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        TaskResponse response = taskService.createTask(orgId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Task created"));
    }

    @Operation(summary = "Get tasks for an organization, optionally filtered by status or assignee")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getTasks(
            @RequestParam UUID orgId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) UUID assignedTo,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<TaskResponse> tasks = taskService.getTasks(orgId, status, assignedTo, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @Operation(summary = "Get a single task by ID")
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTask(taskId, currentUser)));
    }

    @Operation(summary = "Update a task (managers/admins only)")
    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.updateTask(taskId, request, currentUser)));
    }

    @Operation(summary = "Assign or unassign a task (pass null assignedTo to unassign)")
    @PatchMapping("/{taskId}/assign")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody AssignTaskRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.assignTask(taskId, request, currentUser)));
    }

    @Operation(summary = "Change task status")
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<TaskResponse>> changeStatus(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.changeStatus(taskId, request, currentUser)));
    }

    @Operation(summary = "Delete a task (managers/admins only)")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        taskService.deleteTask(taskId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Task deleted"));
    }
}
