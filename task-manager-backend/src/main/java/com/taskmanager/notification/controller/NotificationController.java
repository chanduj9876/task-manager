package com.taskmanager.notification.controller;

import com.taskmanager.common.response.ApiResponse;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.notification.dto.NotificationDto;
import com.taskmanager.notification.service.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Notifications", description = "Retrieve and mark user notifications")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @Operation(summary = "Get all notifications for the current user")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getNotifications(
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getForUser(currentUser, pageable)));
    }

    @Operation(summary = "Get count of unread notifications")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success(notificationService.getUnreadCount(currentUser)));
    }

    @Operation(summary = "Mark a specific notification as read")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success(notificationService.markRead(id, currentUser)));
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        notificationService.markAllRead(currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }
}
