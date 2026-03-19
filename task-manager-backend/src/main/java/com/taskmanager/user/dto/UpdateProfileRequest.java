package com.taskmanager.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 100)
    private String name;

    @Size(min = 6, max = 100)
    private String password;
}
