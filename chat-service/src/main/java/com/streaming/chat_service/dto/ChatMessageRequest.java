package com.streaming.chat_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload from the client via STOMP. Contains only the message body;
 * identity (userId, username) is read from the authenticated WebSocket session
 * and never trusted from client input.
 */
public record ChatMessageRequest(
    @NotBlank(message = "Message content is required")
    @Size(max = 500, message = "Message content must not exceed 500 characters")
    String content
) {}
