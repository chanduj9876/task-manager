package com.taskmanager.task.repository;

import com.taskmanager.task.entity.Task;
import com.taskmanager.task.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByOrgId(UUID orgId, Pageable pageable);

    Page<Task> findByOrgIdAndStatus(UUID orgId, TaskStatus status, Pageable pageable);

    Page<Task> findByOrgIdAndAssignedTo(UUID orgId, UUID assignedTo, Pageable pageable);

    Page<Task> findByOrgIdAndStatusAndAssignedTo(UUID orgId, TaskStatus status, UUID assignedTo, Pageable pageable);

    List<Task> findByOrgId(UUID orgId);

    List<Task> findByOrgIdAndStatus(UUID orgId, TaskStatus status);

    List<Task> findByOrgIdAndAssignedTo(UUID orgId, UUID assignedTo);

    List<Task> findByOrgIdAndStatusAndAssignedTo(UUID orgId, TaskStatus status, UUID assignedTo);
}
