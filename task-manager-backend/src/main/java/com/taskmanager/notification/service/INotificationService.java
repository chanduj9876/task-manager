package com.taskmanager.notification.service;

import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.notification.dto.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface INotificationService {
    Page<NotificationDto> getForUser(UserDetailsImpl currentUser, Pageable pageable);
    NotificationDto markRead(UUID notificationId, UserDetailsImpl currentUser);
    void markAllRead(UserDetailsImpl currentUser);
    long getUnreadCount(UserDetailsImpl currentUser);
}
