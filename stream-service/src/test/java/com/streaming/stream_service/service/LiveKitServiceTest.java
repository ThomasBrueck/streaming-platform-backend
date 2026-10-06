package com.streaming.stream_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class LiveKitServiceTest {

    private static final String API_KEY = "test-key";
    private static final String API_SECRET = "test-secret-at-least-32-bytes-long";

    private final LiveKitService liveKitService = new LiveKitService(
        API_KEY, API_SECRET, "ws://localhost:7880"
    );

    @Test
    void generateIdentity_isDeterministicPerUser() {
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

        String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        assertThat(headerJson).contains("HS256");
    }

    // --- Webhook verification ---

    @Test
    void verifyWebhook_rejectsMissingAuthorization() {
        assertThat(liveKitService.verifyWebhook(null, "{}")).isFalse();
        assertThat(liveKitService.verifyWebhook("", "{}")).isFalse();
    }

    @Test
    void verifyWebhook_rejectsMissingSha256Claim() throws Exception {
        // Build a JWT without the sha256 claim — should be rejected.
        SecretKey key = Keys.hmacShaKeyFor(API_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .issuer(API_KEY)
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(liveKitService.verifyWebhook("Bearer " + token, "{}")).isFalse();
    }

    @Test
    void verifyWebhook_rejectsMismatchedBodyHash() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(API_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .issuer(API_KEY)
                .claim("sha256", "wrong-hash")
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(liveKitService.verifyWebhook("Bearer " + token, "{\"event\":\"test\"}")).isFalse();
    }

    @Test
    void verifyWebhook_acceptsValidSignatureAndBodyHash() throws Exception {
        String body = "{\"event\":\"participant_joined\"}";
        String bodyHash = sha256Base64(body);

        SecretKey key = Keys.hmacShaKeyFor(API_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .issuer(API_KEY)
                .claim("sha256", bodyHash)
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(liveKitService.verifyWebhook("Bearer " + token, body)).isTrue();
    }

    private static String sha256Base64(String body) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
