package com.taskmanager.comment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private UUID taskId;
    private UUID userId;
    private String userName;
    private String content;
    private LocalDateTime createdAt;
}
