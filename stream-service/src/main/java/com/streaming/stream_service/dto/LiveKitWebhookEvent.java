package com.streaming.stream_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Minimal shape of a LiveKit server webhook payload — only the fields this
 * service cares about. See: https://docs.livekit.io/home/server/webhooks/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LiveKitWebhookEvent(
    String event,
    RoomInfo room,
    ParticipantInfo participant
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoomInfo(String sid, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParticipantInfo(String sid, String identity) {}
}
