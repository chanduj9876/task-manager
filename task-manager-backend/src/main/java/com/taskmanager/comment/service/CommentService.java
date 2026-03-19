package com.taskmanager.comment.service;

import com.taskmanager.audit.annotation.Auditable;
import com.taskmanager.comment.dto.CommentRequest;
import com.taskmanager.comment.dto.CommentResponse;
import com.taskmanager.comment.entity.Comment;
import com.taskmanager.comment.repository.CommentRepository;
import com.taskmanager.common.exception.AppException;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.organization.service.IOrganizationService;
import com.taskmanager.task.entity.Task;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService implements ICommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final IOrganizationService organizationService;

    @Transactional
    @Auditable(entityType = "COMMENT", action = "CREATE")
    public CommentResponse addComment(UUID taskId, CommentRequest request,
                                      UserDetailsImpl currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
        organizationService.assertMembership(currentUser.getId(), task.getOrgId(), null);

        Comment comment = Comment.builder()
                .taskId(taskId)
                .userId(currentUser.getId())
                .content(request.getContent())
                .build();
        commentRepository.save(comment);
        return toResponse(comment);
    }

    public List<CommentResponse> getComments(UUID taskId, UserDetailsImpl currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
        organizationService.assertMembership(currentUser.getId(), task.getOrgId(), null);

        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    @Auditable(entityType = "COMMENT", action = "DELETE", entityClass = com.taskmanager.comment.entity.Comment.class)
    public void deleteComment(UUID commentId, UserDetailsImpl currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment not found", HttpStatus.NOT_FOUND));

        boolean isOwner = comment.getUserId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) {
            throw new AppException("You can only delete your own comments", HttpStatus.FORBIDDEN);
        }
        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(Comment comment) {
        String userName = userRepository.findById(comment.getUserId())
                .map(User::getName).orElse("Unknown");
        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .userId(comment.getUserId())
                .userName(userName)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
