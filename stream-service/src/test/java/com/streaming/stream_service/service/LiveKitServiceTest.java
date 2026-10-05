package com.streaming.stream_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LiveKitServiceTest {

    private final LiveKitService liveKitService = new LiveKitService(
        "test-key",
        "test-secret-at-least-32-bytes-long",
        "ws://localhost:7880"
    );

    @Test
    void generateIdentity_isDeterministicPerUser() {
        // Same user reconnecting (refresh, second tab) must resolve to the same
        // LiveKit identity — otherwise each reconnect looks like a new viewer.
        String first = liveKitService.generateIdentity(42L);
        String second = liveKitService.generateIdentity(42L);

        assertThat(first).isEqualTo(second);
        assertThat(first).isEqualTo("user-42");
    }

    @Test
    void generateIdentity_differsAcrossUsers() {
        assertThat(liveKitService.generateIdentity(1L)).isNotEqualTo(liveKitService.generateIdentity(2L));
    }

    @Test
    void createAccessToken_signsWithHS256() {
        String token = liveKitService.createAccessToken("user-1", "Alice", "stream_1", true);

        // A JWT header only reveals the algorithm and type, never the secret —
        // safe to decode without verifying for this structural check.
        String headerJson = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        assertThat(headerJson).contains("HS256");
    }
}
