package com.taskmanager.comment.controller;

import com.taskmanager.comment.dto.CommentRequest;
import com.taskmanager.comment.dto.CommentResponse;
import com.taskmanager.comment.service.ICommentService;
import com.taskmanager.common.response.ApiResponse;
import com.taskmanager.common.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Comments", description = "Add, list, and delete task comments")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final ICommentService commentService;

    @Operation(summary = "Add a comment to a task")
    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        CommentResponse response = commentService.addComment(taskId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Comment added"));
    }

    @Operation(summary = "Get all comments for a task")
    @GetMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<CommentResponse> comments = commentService.getComments(taskId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @Operation(summary = "Delete a comment")
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted"));
    }
}
