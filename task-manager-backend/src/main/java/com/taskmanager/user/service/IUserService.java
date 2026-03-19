package com.taskmanager.user.service;

import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.user.dto.UpdateProfileRequest;
import com.taskmanager.user.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    UserResponse getById(UUID id);
    UserResponse getByEmail(String email);

    UserResponse updateMe(UpdateProfileRequest request, UserDetailsImpl currentUser);

    List<UserResponse> getAllUsers();
}
