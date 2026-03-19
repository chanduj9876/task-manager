package com.taskmanager.user.service;

import com.taskmanager.common.exception.AppException;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.user.dto.UpdateProfileRequest;
import com.taskmanager.user.dto.UserResponse;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return toResponse(user);
    }

    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return toResponse(user);
    }

    public UserResponse updateMe(UpdateProfileRequest request, UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (request.getName() != null) {
            String trimmed = request.getName().trim();
            if (!trimmed.isBlank()) {
                user.setName(trimmed);
            }
        }

        if (request.getPassword() != null) {
            String trimmed = request.getPassword().trim();
            if (!trimmed.isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(trimmed));
            }
        }

        userRepository.save(user);
        return toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserService::toResponse).toList();
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
