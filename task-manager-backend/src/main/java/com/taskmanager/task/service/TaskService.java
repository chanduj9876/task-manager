package com.taskmanager.task.service;

import com.taskmanager.audit.annotation.Auditable;
import com.taskmanager.common.exception.AppException;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.notification.dto.TaskEventDto;
import com.taskmanager.notification.kafka.NotificationProducer;
import com.taskmanager.organization.service.IOrganizationService;
import com.taskmanager.task.dto.*;
import com.taskmanager.task.entity.Task;
import com.taskmanager.task.enums.TaskStatus;
import com.taskmanager.task.factory.TaskFactory;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.enums.Role;
import com.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService implements ITaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final IOrganizationService organizationService;
    private final NotificationProducer notificationProducer;

    @Transactional
    @Auditable(entityType = "TASK", action = "CREATE")
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse createTask(UUID orgId, TaskRequest request, UserDetailsImpl currentUser) {
        organizationService.assertMembership(currentUser.getId(), orgId, null);

        if (request.getAssignedTo() != null) {
            assertManagerOrAdmin(currentUser.getId(), orgId);
            assertOrgMember(request.getAssignedTo(), orgId);
        }

        Task task = TaskFactory.createTask(request, orgId, currentUser.getId());
        taskRepository.save(task);

        String actorName = userRepository.findById(currentUser.getId())
                .map(User::getName).orElse("Unknown");
        notificationProducer.publishTaskEvent(TaskEventDto.builder()
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .eventType("TASK_CREATED")
                .actorId(currentUser.getId())
                .actorName(actorName)
                .orgId(orgId)
                .assigneeId(task.getAssignedTo())
                .build());

        return toResponse(task);
    }

    @Transactional
    @Auditable(entityType = "TASK", action = "UPDATE", entityClass = com.taskmanager.task.entity.Task.class)
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse updateTask(UUID taskId, TaskRequest request, UserDetailsImpl currentUser) {
        Task task = getTaskAndAssertAccess(taskId, currentUser);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        taskRepository.save(task);

        String actorName = userRepository.findById(currentUser.getId())
                .map(User::getName).orElse("Unknown");
        notificationProducer.publishTaskEvent(TaskEventDto.builder()
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .eventType("TASK_UPDATED")
                .actorId(currentUser.getId())
                .actorName(actorName)
                .orgId(task.getOrgId())
                .assigneeId(task.getAssignedTo())
                .build());

        return toResponse(task);
    }

    @Transactional
    @Auditable(entityType = "TASK", action = "ASSIGN", entityClass = com.taskmanager.task.entity.Task.class)
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse assignTask(UUID taskId, AssignTaskRequest request,
                                   UserDetailsImpl currentUser) {
        Task task = getExistingTask(taskId);
        assertManagerOrAdmin(currentUser.getId(), task.getOrgId());
        if (request.getAssignedTo() != null) {
            assertOrgMember(request.getAssignedTo(), task.getOrgId());
        }

        task.setAssignedTo(request.getAssignedTo());
        taskRepository.save(task);

        String actorName = userRepository.findById(currentUser.getId())
                .map(User::getName).orElse("Unknown");
        notificationProducer.publishTaskEvent(TaskEventDto.builder()
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .eventType("TASK_ASSIGNED")
                .actorId(currentUser.getId())
                .actorName(actorName)
                .orgId(task.getOrgId())
                .assigneeId(request.getAssignedTo())
                .build());

        return toResponse(task);
    }

    @Transactional
    @Auditable(entityType = "TASK", action = "STATUS_CHANGE", entityClass = com.taskmanager.task.entity.Task.class)
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse changeStatus(UUID taskId, UpdateStatusRequest request,
                                     UserDetailsImpl currentUser) {
        Task task = getTaskAndAssertAccess(taskId, currentUser);
        task.setStatus(request.getStatus());
        taskRepository.save(task);

        String actorName = userRepository.findById(currentUser.getId())
                .map(User::getName).orElse("Unknown");
        notificationProducer.publishTaskEvent(TaskEventDto.builder()
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .eventType("STATUS_CHANGED")
                .actorId(currentUser.getId())
                .actorName(actorName)
                .orgId(task.getOrgId())
                .assigneeId(task.getAssignedTo())
                .build());

        return toResponse(task);
    }

    @Transactional
    @Auditable(entityType = "TASK", action = "DELETE", entityClass = com.taskmanager.task.entity.Task.class)
    @CacheEvict(value = "tasks", allEntries = true)
    public void deleteTask(UUID taskId, UserDetailsImpl currentUser) {
        Task task = getExistingTask(taskId);
        assertManagerOrAdmin(currentUser.getId(), task.getOrgId());
        taskRepository.delete(task);
    }

    public TaskResponse getTask(UUID taskId, UserDetailsImpl currentUser) {
        Task task = getExistingTask(taskId);
        organizationService.assertMembership(currentUser.getId(), task.getOrgId(), null);
        return toResponse(task);
    }

    @Cacheable(
            value = "tasks",
            key = "#currentUser.id + ':' + #orgId + ':' + #status + ':' + #assignedTo + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()"
    )
    public Page<TaskResponse> getTasks(UUID orgId, TaskStatus status, UUID assignedTo,
                                       Pageable pageable,
                                       UserDetailsImpl currentUser) {
        organizationService.assertMembership(currentUser.getId(), orgId, null);

        Page<Task> tasks;
        if (status != null && assignedTo != null) {
            tasks = taskRepository.findByOrgIdAndStatusAndAssignedTo(orgId, status, assignedTo, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByOrgIdAndStatus(orgId, status, pageable);
        } else if (assignedTo != null) {
            tasks = taskRepository.findByOrgIdAndAssignedTo(orgId, assignedTo, pageable);
        } else {
            tasks = taskRepository.findByOrgId(orgId, pageable);
        }

        return tasks.map(this::toResponse);
    }

    private Task getExistingTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
    }

    private Task getTaskAndAssertAccess(UUID taskId, UserDetailsImpl currentUser) {
        Task task = getExistingTask(taskId);
        organizationService.assertMembership(currentUser.getId(), task.getOrgId(), null);
        // MEMBER can only modify tasks assigned to them or created by them
        boolean isManagerOrAdmin = organizationService.isManagerOrAdminInOrg(currentUser.getId(), task.getOrgId());
        boolean isOwnerOrAssignee = task.getCreatedBy().equals(currentUser.getId())
                || currentUser.getId().equals(task.getAssignedTo());
        if (!isManagerOrAdmin && !isOwnerOrAssignee) {
            throw new AppException("You do not have permission to modify this task",
                    HttpStatus.FORBIDDEN);
        }
        return task;
    }

    private void assertManagerOrAdmin(UUID userId, UUID orgId) {
        // Delegates to org service — ADMIN is the strictest required here, but MANAGER also passes
        try {
            organizationService.assertMembership(userId, orgId, Role.ADMIN);
        } catch (AppException e) {
            try {
                organizationService.assertMembership(userId, orgId, Role.MANAGER);
            } catch (AppException ex) {
                throw new AppException("Only ADMIN or MANAGER can perform this action",
                        HttpStatus.FORBIDDEN);
            }
        }
    }

    private void assertOrgMember(UUID userId, UUID orgId) {
        organizationService.assertMembership(userId, orgId, null);
    }

    public TaskResponse toResponse(Task task) {
        String assignedToName = null;
        if (task.getAssignedTo() != null) {
            assignedToName = userRepository.findById(task.getAssignedTo())
                    .map(User::getName).orElse(null);
        }
        String createdByName = userRepository.findById(task.getCreatedBy())
                .map(User::getName).orElse(null);

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .orgId(task.getOrgId())
                .assignedTo(task.getAssignedTo())
                .assignedToName(assignedToName)
                .createdBy(task.getCreatedBy())
                .createdByName(createdByName)
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
