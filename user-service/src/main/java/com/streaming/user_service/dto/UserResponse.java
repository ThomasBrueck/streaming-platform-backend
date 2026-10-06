package com.streaming.user_service.dto;

import java.time.LocalDateTime;

/**
 * Public user profile — email is deliberately excluded to prevent leaking
 * personal data to any authenticated caller.
 */
public record UserResponse(
    Long id,
    String username,
    String displayName,
    LocalDateTime createdAt
) {}