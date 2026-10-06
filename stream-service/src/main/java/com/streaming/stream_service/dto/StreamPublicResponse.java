package com.streaming.stream_service.dto;

import java.time.LocalDateTime;

import com.streaming.stream_service.enums.StreamCategory;
import com.streaming.stream_service.enums.StreamStatus;

/**
 * Public-facing stream representation — never includes the stream key,
 * which is a secret the owner uses for RTMP/LiveKit ingest.
 */
public record StreamPublicResponse(
    Long id,
    Long userId,
    String title,
    String description,
    StreamCategory category,
    StreamStatus status,
    int viewerCount,
    LocalDateTime createdAt
) {}
