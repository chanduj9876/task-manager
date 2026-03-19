package com.taskmanager.organization.repository;

import com.taskmanager.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    @Query("SELECT o FROM Organization o JOIN UserOrganization uo ON o.id = uo.orgId WHERE uo.userId = :userId AND uo.status = 'ACTIVE'")
    List<Organization> findAllByUserId(UUID userId);
}
