package com.taskmanager.task;

import com.taskmanager.common.exception.AppException;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.notification.kafka.NotificationProducer;
import com.taskmanager.organization.service.IOrganizationService;
import com.taskmanager.task.dto.AssignTaskRequest;
import com.taskmanager.task.dto.TaskRequest;
import com.taskmanager.task.dto.TaskResponse;
import com.taskmanager.task.dto.UpdateStatusRequest;
import com.taskmanager.task.entity.Task;
import com.taskmanager.task.enums.TaskPriority;
import com.taskmanager.task.enums.TaskStatus;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.task.service.TaskService;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.enums.Role;
import com.taskmanager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private IOrganizationService organizationService;
    @Mock private NotificationProducer notificationProducer;

    @InjectMocks
    private TaskService taskService;

    private UUID orgId;
    private UUID adminUserId;
    private UserDetailsImpl adminUserDetails;
    private Task existingTask;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();

        adminUserDetails = mock(UserDetailsImpl.class);
        when(adminUserDetails.getId()).thenReturn(adminUserId);
        when(adminUserDetails.getAuthorities()).thenReturn(
                (Collection) List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        existingTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Test Task")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .orgId(orgId)
                .createdBy(adminUserId)
                .build();
    }

    @Test
    void createTask_success() {
        TaskRequest request = new TaskRequest();
        request.setTitle("New Task");
        request.setPriority(TaskPriority.HIGH);

        doNothing().when(organizationService).assertMembership(any(), any(), any());
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(
                User.builder().id(adminUserId).name("Admin").build()));

        TaskResponse response = taskService.createTask(orgId, request, adminUserDetails);

        assertThat(response.getTitle()).isEqualTo("New Task");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        verify(taskRepository).save(any(Task.class));
        verify(notificationProducer).publishTaskEvent(any());
    }

    @Test
    void changeStatus_success() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(existingTask.getId())).thenReturn(Optional.of(existingTask));
        doNothing().when(organizationService).assertMembership(any(), any(), any());
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(
                User.builder().id(adminUserId).name("Admin").build()));

        TaskResponse response = taskService.changeStatus(existingTask.getId(), request,
                adminUserDetails);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(notificationProducer).publishTaskEvent(any());
    }

    @Test
    void getTask_notFound_throws() {
        UUID randomId = UUID.randomUUID();
        when(taskRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(randomId, adminUserDetails))
                .isInstanceOf(AppException.class);
    }

    @Test
    void deleteTask_memberRole_throwsForbidden() {
        UserDetailsImpl memberUser = mock(UserDetailsImpl.class);
        when(memberUser.getId()).thenReturn(UUID.randomUUID());
        when(memberUser.getAuthorities()).thenReturn(
                (Collection) List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_MEMBER")));

        when(taskRepository.findById(existingTask.getId())).thenReturn(Optional.of(existingTask));
        doThrow(new AppException("Insufficient permissions", org.springframework.http.HttpStatus.FORBIDDEN))
                .when(organizationService).assertMembership(any(), any(), eq(Role.ADMIN));
        doThrow(new AppException("Insufficient permissions", org.springframework.http.HttpStatus.FORBIDDEN))
                .when(organizationService).assertMembership(any(), any(), eq(Role.MANAGER));

        assertThatThrownBy(() -> taskService.deleteTask(existingTask.getId(), memberUser))
                .isInstanceOf(AppException.class);
    }
}
