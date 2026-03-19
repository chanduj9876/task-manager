package com.taskmanager.audit.service;

import com.taskmanager.audit.entity.AuditLog;
import com.taskmanager.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves audit log entries in an independent transaction (REQUIRES_NEW) so that
 * audit writes succeed even when the parent transaction rolls back, and a failed
 * audit write never rolls back the parent business transaction.
 */
@Service
@RequiredArgsConstructor
public class AuditPersistenceService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }
}
