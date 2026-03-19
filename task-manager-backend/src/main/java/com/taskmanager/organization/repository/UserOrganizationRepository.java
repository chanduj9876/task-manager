package com.taskmanager.organization.repository;

import com.taskmanager.organization.entity.InvitationStatus;
import com.taskmanager.organization.entity.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserOrganizationRepository
        extends JpaRepository<UserOrganization, UserOrganization.UserOrgId> {

    List<UserOrganization> findByOrgId(UUID orgId);

    List<UserOrganization> findByUserIdAndStatus(UUID userId, InvitationStatus status);

    Optional<UserOrganization> findByUserIdAndOrgId(UUID userId, UUID orgId);

    boolean existsByUserIdAndOrgId(UUID userId, UUID orgId);
}
