package com.streaming.stream_service.dto;

import java.time.LocalDateTime;

import com.streaming.stream_service.enums.StreamStatus;

public record StreamResponse (
    Long id,
    Long userID,
    String title,
    String description,
    String category,
    StreamStatus status,
    String streamKey,
    int viewerCount,
    LocalDateTime createdAt
) {}
