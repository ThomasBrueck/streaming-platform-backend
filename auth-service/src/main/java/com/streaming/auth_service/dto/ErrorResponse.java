package com.streaming.auth_service.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    String message,
    int status,
    LocalDateTime createdAt
) {}
