package com.streaming.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username needs minimum 3 and maximum 50 characters")
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Email isn't valid")
    String email,

    @Size(max = 100)
    String displayName
) {}
