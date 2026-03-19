package com.taskmanager.auth.service;

import com.taskmanager.auth.dto.AuthResponse;
import com.taskmanager.auth.dto.LoginRequest;
import com.taskmanager.auth.dto.SignupRequest;

public interface IAuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String token);
}
