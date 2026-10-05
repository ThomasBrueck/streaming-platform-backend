package com.streaming.stream_service.dto;

import jakarta.validation.constraints.Size;

/**
 * Optional body when requesting a LiveKit token.
 * canPublish is NEVER trusted from the client — it is derived server-side
 * by comparing the caller's user id against the stream owner.
 */
public record LiveKitTokenRequest(
    @Size(max = 60, message = "Display name max 60 characters")
    String displayName
) {}
