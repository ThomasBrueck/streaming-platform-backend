package com.streaming.stream_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streaming.stream_service.dto.CreateStreamRequest;
import com.streaming.stream_service.dto.StreamResponse;
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
    public ResponseEntity<StreamResponse> createStream(@Valid @RequestBody CreateStreamRequest request) {
        StreamResponse streamCreated = streamService.createStream(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(streamCreated);
    }

    @GetMapping
    public List<StreamResponse> getAllStreams() {
        return streamService.getAllStreams();
    }

    @GetMapping("/{id}")
    public StreamResponse getStreamById(@PathVariable Long id) {
        return streamService.getStreamById(id);
    }

    @GetMapping("/user/{userId}")
    public StreamResponse getStreamByUser(@PathVariable Long userId) {
        return streamService.getStreamByUserId(userId);
    }

    @PatchMapping("/{id}/status")
    public StreamResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStreamStatusRequest request) {
        return streamService.updateStreamStatusById(id, request.status());
    }
}
