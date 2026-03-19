package com.taskmanager.comment.service;

import com.taskmanager.comment.dto.CommentRequest;
import com.taskmanager.comment.dto.CommentResponse;
import com.taskmanager.common.security.UserDetailsImpl;

import java.util.List;
import java.util.UUID;

public interface ICommentService {
    CommentResponse addComment(UUID taskId, CommentRequest request, UserDetailsImpl currentUser);
    List<CommentResponse> getComments(UUID taskId, UserDetailsImpl currentUser);
    void deleteComment(UUID commentId, UserDetailsImpl currentUser);
}
