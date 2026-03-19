package com.taskmanager.organization.entity;

import com.taskmanager.user.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_organizations", indexes = {
        @Index(name = "idx_user_org_user_id", columnList = "user_id"),
        @Index(name = "idx_user_org_status",  columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserOrganization.UserOrgId.class)
public class UserOrganization {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "org_id")
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'ACTIVE'")
    @Builder.Default
    private InvitationStatus status = InvitationStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserOrgId implements Serializable {
        private UUID userId;
        private UUID orgId;
    }
}
