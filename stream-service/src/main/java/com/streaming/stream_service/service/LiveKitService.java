package com.streaming.stream_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Mints and verifies LiveKit (WebRTC SFU) credentials.
 *
 * This is the ONLY place in the system that knows the LiveKit API secret.
 * Clients (browsers) must never receive it — they only ever receive a
 * short-lived, scoped access token produced here.
 */
@Service
public class LiveKitService {

    private static final Logger log = LoggerFactory.getLogger(LiveKitService.class);
    private static final long TOKEN_TTL_MS = 60 * 60 * 1000; // 1 hour

    private final SecretKey key;
    private final String apiKey;
    private final String wsUrl;

    public LiveKitService(
        @Value("${livekit.api-key}") String apiKey,
        @Value("${livekit.api-secret}") String apiSecret,
        @Value("${livekit.ws-url}") String wsUrl
    ) {
        this.apiKey = apiKey;
        this.wsUrl = wsUrl;
        this.key = Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getWsUrl() {
        return wsUrl;
    }

    /**
     * Builds a scoped LiveKit access token. {@code canPublish} must be decided
     * by the caller from trusted server-side state (e.g. stream ownership) —
     * never from client input.
     */
    public String createAccessToken(String identity, String displayName, String room, boolean canPublish) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + TOKEN_TTL_MS);

        Map<String, Object> videoGrant = Map.of(
            "room", room,
            "roomJoin", true,
            "canPublish", canPublish,
            "canSubscribe", true,
            "canPublishData", true
        );

        return Jwts.builder()
            .issuer(apiKey)
            .subject(identity)
            .claim("name", displayName)
            .claim("video", videoGrant)
            .issuedAt(now)
            .notBefore(now)
            .expiration(expiry)
            // LiveKit's server hardcodes HS256 for access tokens — jjwt's
            // no-arg signWith(key) auto-upgrades to HS384/HS512 once the key
            // is long enough, which LiveKit then rejects. Pin it explicitly.
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }

    /**
     * Deterministic per-user identity — deliberately NOT random. LiveKit treats
     * "identity" as a single connection slot per room: if the same user opens
     * the stream again (another tab, a refresh), the new connection replaces
     * the old one under this same identity instead of registering as a second
     * participant. That's what keeps the viewer count counting people, not tabs.
     */
    public String generateIdentity(Long userId) {
        return "user-" + userId;
    }

    /**
     * Verifies a LiveKit webhook call: the Authorization header must carry a JWT
     * signed with our own API secret (issued by the LiveKit server itself), whose
     * `sha256` claim matches the SHA-256 hash of the raw request body.
     */
    public boolean verifyWebhook(String authorizationHeader, String rawBody) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.warn("livekit webhook rejected: missing Authorization header");
            return false;
        }

        String token = authorizationHeader.startsWith("Bearer ")
            ? authorizationHeader.substring(7)
            : authorizationHeader;

        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            Claims claims = jws.getPayload();

            if (!apiKey.equals(claims.getIssuer())) {
                log.warn("livekit webhook rejected: issuer mismatch");
                return false;
            }

            String claimedHash = claims.get("sha256", String.class);
            if (claimedHash == null || claimedHash.isBlank()) {
                log.warn("livekit webhook rejected: missing sha256 body-hash claim");
                return false;
            }

            String actualHash = sha256Base64(rawBody);
            if (!claimedHash.equals(actualHash)) {
                log.warn("livekit webhook rejected: body hash mismatch");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("livekit webhook rejected: {}", e.getMessage());
            return false;
        }
    }

    private String sha256Base64(String body) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
