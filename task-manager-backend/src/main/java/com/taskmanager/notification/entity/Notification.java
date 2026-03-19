package com.taskmanager.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_read",    columnList = "read")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String eventType;

    private UUID relatedTaskId;

    private UUID relatedOrgId;

    @Column(nullable = false)
    private boolean read;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
