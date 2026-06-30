package com.streaming.auth_service.dto;

public record RegisterResponse(
    Long id,
    String username,
    String email,
    String role
) {}
