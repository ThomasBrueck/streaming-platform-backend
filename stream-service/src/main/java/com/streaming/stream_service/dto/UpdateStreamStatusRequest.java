package com.streaming.stream_service.dto;

import com.streaming.stream_service.enums.StreamStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateStreamStatusRequest (

    @NotNull(message = "status is required")
    StreamStatus status
) {}
