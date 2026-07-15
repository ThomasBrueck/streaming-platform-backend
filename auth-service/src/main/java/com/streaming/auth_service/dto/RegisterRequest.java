package com.streaming.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Email String email,
    @Size(max = 50) String displayName,
    @NotBlank @Size(min = 6, max = 100) String password,
    @NotBlank @Size(min = 6, max = 100) String passwordConfirm

) {}
