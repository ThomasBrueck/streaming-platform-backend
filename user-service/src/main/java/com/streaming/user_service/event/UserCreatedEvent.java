package com.streaming.user_service.event;

import java.time.LocalDateTime;

public record UserCreatedEvent(
    Long userId,
    String email,
    String username,
    String displayName,
    LocalDateTime createdAt
) {}
