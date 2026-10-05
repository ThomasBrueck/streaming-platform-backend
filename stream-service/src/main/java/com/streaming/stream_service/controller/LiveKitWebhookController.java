package com.streaming.stream_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.stream_service.dto.LiveKitWebhookEvent;
import com.streaming.stream_service.service.LiveKitService;
import com.streaming.stream_service.service.StreamService;

/**
 * Receives server-to-server webhooks from LiveKit (never called by the browser
 * and NOT routed through the API gateway — LiveKit hits this service directly
 * over the internal Docker network). Used to keep viewerCount accurate without
 * trusting the client. Every call is signature-verified.
 */
@RestController
@RequestMapping("/api/streams/webhook")
public class LiveKitWebhookController {

    private static final Logger log = LoggerFactory.getLogger(LiveKitWebhookController.class);

    private final LiveKitService liveKitService;
    private final StreamService streamService;
    private final ObjectMapper objectMapper;

    public LiveKitWebhookController(LiveKitService liveKitService, StreamService streamService, ObjectMapper objectMapper) {
        this.liveKitService = liveKitService;
        this.streamService = streamService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/livekit")
    public ResponseEntity<Void> handleWebhook(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody String rawBody
    ) {
        if (!liveKitService.verifyWebhook(authorization, rawBody)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            LiveKitWebhookEvent event = objectMapper.readValue(rawBody, LiveKitWebhookEvent.class);
            String roomName = event.room() != null ? event.room().name() : null;

            switch (event.event()) {
                case "participant_joined" -> streamService.onParticipantJoined(roomName);
                case "participant_left" -> streamService.onParticipantLeft(roomName);
                case "room_finished" -> streamService.onRoomFinished(roomName);
                default -> log.debug("ignoring livekit event: {}", event.event());
            }
        } catch (Exception e) {
            log.warn("failed to process livekit webhook payload: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
