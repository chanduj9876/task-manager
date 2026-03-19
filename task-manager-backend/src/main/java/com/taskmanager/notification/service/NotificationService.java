package com.taskmanager.notification.service;

import com.taskmanager.common.exception.AppException;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.notification.dto.NotificationDto;
import com.taskmanager.notification.entity.Notification;
import com.taskmanager.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Singleton Pattern: Spring @Service creates a single shared instance.
 */
@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;

    public Page<NotificationDto> getForUser(UserDetailsImpl currentUser, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(this::toDto);
    }

    @Transactional
    public NotificationDto markRead(UUID notificationId, UserDetailsImpl currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException("Notification not found", HttpStatus.NOT_FOUND));

        if (!notification.getUserId().equals(currentUser.getId())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return toDto(notification);
    }

    @Transactional
    public void markAllRead(UserDetailsImpl currentUser) {
        List<Notification> unread = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().filter(n -> !n.isRead()).toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public long getUnreadCount(UserDetailsImpl currentUser) {
        return notificationRepository.countByUserIdAndReadFalse(currentUser.getId());
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .message(n.getMessage())
                .eventType(n.getEventType())
                .relatedTaskId(n.getRelatedTaskId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
