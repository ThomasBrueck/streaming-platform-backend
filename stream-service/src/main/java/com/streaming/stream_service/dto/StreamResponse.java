package com.streaming.stream_service.dto;

import java.time.LocalDateTime;

import com.streaming.stream_service.enums.StreamCategory;
import com.streaming.stream_service.enums.StreamStatus;

public record StreamResponse (
    Long id,
    Long userID,
    String title,
    String description,
    StreamCategory category,
    StreamStatus status,
    String streamKey,
    int viewerCount,
    LocalDateTime createdAt
) {}
