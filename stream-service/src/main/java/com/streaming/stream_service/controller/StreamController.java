package com.streaming.stream_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.streaming.stream_service.dto.CreateStreamRequest;
import com.streaming.stream_service.dto.LiveKitTokenRequest;
import com.streaming.stream_service.dto.LiveKitTokenResponse;
import com.streaming.stream_service.dto.StreamPublicResponse;
import com.streaming.stream_service.dto.StreamResponse;
import com.streaming.stream_service.dto.UpdateStreamRequest;
import com.streaming.stream_service.dto.UpdateStreamStatusRequest;
import com.streaming.stream_service.service.StreamService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/streams")
public class StreamController {
    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping
    public ResponseEntity<StreamResponse> createStream(@Valid @RequestBody CreateStreamRequest request, @RequestHeader("user_id") Long requestUserId) {
        StreamResponse streamCreated = streamService.createStream(request, requestUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(streamCreated);
    }

    @GetMapping
    public Page<StreamPublicResponse> getAllStreams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return streamService.getAllStreams(pageable);
    }

    @GetMapping("/{id}")
    public StreamPublicResponse getStreamById(@PathVariable Long id) {
        return streamService.getStreamById(id);
    }

    @GetMapping("/user/{userId}")
    public StreamPublicResponse getStreamByUser(@PathVariable Long userId) {
        return streamService.getStreamByUserId(userId);
    }

    @PatchMapping("/{id}/status")
    public StreamResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStreamStatusRequest request, @RequestHeader("user_id") Long requestUserId) {
        return streamService.updateStreamStatusById(id, request.status(), requestUserId);
    }

    /** Lets the owner edit their channel's title/description/category — there is only ever one. */
    @PatchMapping("/{id}")
    public StreamResponse updateStream(@PathVariable Long id, @Valid @RequestBody UpdateStreamRequest request, @RequestHeader("user_id") Long requestUserId) {
        return streamService.updateStream(id, request, requestUserId);
    }

    /**
     * Issues a scoped LiveKit access token for this stream's room. This is the
     * ONLY way a client obtains WebRTC credentials — the LiveKit secret never
     * reaches the browser. Publish rights are granted only to the stream owner.
     */
    @PostMapping("/{id}/token")
    public LiveKitTokenResponse createLiveKitToken(
        @PathVariable Long id,
        @RequestHeader("user_id") Long requestUserId,
        @RequestBody(required = false) LiveKitTokenRequest request
    ) {
        String displayName = request != null ? request.displayName() : null;
        return streamService.createLiveKitToken(id, requestUserId, displayName);
    }
}
