package com.streaming.stream_service.dto;

public record LiveKitTokenResponse(
    String token,
    String url,
    String roomName,
    String identity,
    boolean canPublish
) {}
