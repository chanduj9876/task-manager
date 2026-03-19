package com.taskmanager.audit.controller;

import com.taskmanager.audit.dto.AuditLogResponse;
import com.taskmanager.audit.entity.AuditLog;
import com.taskmanager.audit.repository.AuditLogRepository;
import com.taskmanager.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Logs", description = "View audit logs (ADMIN only)")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @Operation(summary = "Query audit logs by entity type (and optionally entity ID). ADMIN only.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam String entityType,
            @RequestParam(required = false) String entityId,
            Pageable pageable) {
        Page<AuditLog> logs = (entityId != null && !entityId.isBlank())
            ? auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId, pageable)
            : auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs.map(this::toResponse)));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .performedBy(log.getPerformedBy())
                .performedByName(log.getPerformedByName())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }
}

