package com.streaming.chat_service.dto;

public record ChatMessage(
    String streamId,
    String userId,
    String username,
    String content
) {}
