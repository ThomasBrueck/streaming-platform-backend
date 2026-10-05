package com.streaming.stream_service.service;


import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streaming.stream_service.dto.CreateStreamRequest;
import com.streaming.stream_service.dto.LiveKitTokenResponse;
import com.streaming.stream_service.dto.StreamResponse;
import com.streaming.stream_service.dto.UpdateStreamRequest;
import com.streaming.stream_service.entity.Stream;
import com.streaming.stream_service.enums.StreamStatus;
import com.streaming.stream_service.exception.DuplicateStreamException;
import com.streaming.stream_service.exception.StreamNotFoundException;
import com.streaming.stream_service.exception.UnauthorizedMethod;
import com.streaming.stream_service.mapper.StreamMapper;
import com.streaming.stream_service.repository.StreamRepository;

@Service
public class StreamService {

    private static final Logger log = LoggerFactory.getLogger(StreamService.class);
    private static final String ROOM_PREFIX = "stream_";

    private final StreamRepository streamRepository;
    private final StreamMapper streamMapper;
    private final LiveKitService liveKitService;

    public StreamService(StreamRepository streamRepository, StreamMapper streamMapper, LiveKitService liveKitService) {
        this.streamRepository = streamRepository;
        this.streamMapper = streamMapper;
        this.liveKitService = liveKitService;
    }

    /**
     * A user gets exactly one channel, ever — creating a second one is rejected.
     * Update the existing channel (see {@link #updateStream}) instead of
     * spawning a new row every time someone wants to go live.
     */
    @Transactional
    public StreamResponse createStream(CreateStreamRequest request, Long userId) {
        if (streamRepository.findByUserId(userId).isPresent()) {
            throw new DuplicateStreamException("You already have a channel — edit it instead of creating a new one.");
        }

        Stream stream = streamMapper.toEntity(request, userId);
        stream.setStreamKey(generateStreamKey());

        Stream saved = streamRepository.save(stream);
        return streamMapper.toResponse(saved);
    }

    @Transactional
    public StreamResponse updateStream(Long id, UpdateStreamRequest request, Long userId) {
        Stream stream = streamRepository.findById(id).orElseThrow(() -> new StreamNotFoundException("stream not found: " + id));

        if (!stream.getUserId().equals(userId)) {
            throw new UnauthorizedMethod("you don't have permission to do this");
        }

        stream.setTitle(request.title());
        stream.setDescription(request.description());
        stream.setCategory(request.category() != null ? request.category() : stream.getCategory());

        Stream saved = streamRepository.save(stream);
        return streamMapper.toResponse(saved);
    }

    private String generateStreamKey() {
        return "live_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional
    public StreamResponse updateStreamStatusById(Long id, StreamStatus status, Long userId) {
        Stream stream = streamRepository.findById(id).orElseThrow(() -> new StreamNotFoundException("stream not found: " + id));

        if (!stream.getUserId().equals(userId)) {
            throw new UnauthorizedMethod("you don't have permission to do this");
        }

        stream.setStatus(status);
        if (status == StreamStatus.OFFLINE) {
            stream.setViewerCount(0);
        }
        streamRepository.save(stream);

        StreamResponse updatedStream = streamMapper.toResponse(stream);

        return updatedStream;
    }

    @Transactional(readOnly = true)
    public List<StreamResponse> getAllStreams() {
        return streamRepository.findAll()
            .stream()
            .map(streamMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public StreamResponse getStreamById(Long id) {
        Stream stream = streamRepository.findById(id).orElseThrow(() -> new StreamNotFoundException("stream not found: " + id));
        StreamResponse streamResponse = streamMapper.toResponse(stream);

        return streamResponse;
    }

    @Transactional(readOnly = true)
    public StreamResponse getStreamByUserId(Long userId) {
        Stream stream = streamRepository.findByUserId(userId).orElseThrow(() -> new StreamNotFoundException("stream not found with user id: " + userId));
        StreamResponse streamResponse = streamMapper.toResponse(stream);

        return streamResponse;
    }

    @Transactional
    public void deleteStreamsByUserId(Long userId) {
        long deleted = streamRepository.deleteByUserId(userId);
        log.info("deleted streams of user id: {}. quantity: {}", userId, deleted);
    }

    /**
     * Issues a scoped LiveKit access token for a stream's room. Publish rights
     * are granted ONLY when the requester owns the stream — this is the single
     * point of trust for who is allowed to broadcast.
     */
    @Transactional(readOnly = true)
    public LiveKitTokenResponse createLiveKitToken(Long streamId, Long requestUserId, String displayName) {
        Stream stream = streamRepository.findById(streamId)
            .orElseThrow(() -> new StreamNotFoundException("stream not found: " + streamId));

        boolean canPublish = stream.getUserId().equals(requestUserId);
        String room = ROOM_PREFIX + stream.getId();
        String identity = liveKitService.generateIdentity(requestUserId);
        String name = (displayName == null || displayName.isBlank()) ? identity : displayName;

        String token = liveKitService.createAccessToken(identity, name, room, canPublish);

        return new LiveKitTokenResponse(token, liveKitService.getWsUrl(), room, identity, canPublish);
    }

    /** Called from the LiveKit webhook when a participant joins a stream's room. */
    @Transactional
    public void onParticipantJoined(String roomName) {
        parseStreamId(roomName).ifPresent(id ->
            streamRepository.findById(id).ifPresent(stream -> {
                stream.setViewerCount(stream.getViewerCount() + 1);
                streamRepository.save(stream);
            })
        );
    }

    /** Called from the LiveKit webhook when a participant leaves a stream's room. */
    @Transactional
    public void onParticipantLeft(String roomName) {
        parseStreamId(roomName).ifPresent(id ->
            streamRepository.findById(id).ifPresent(stream -> {
                stream.setViewerCount(Math.max(0, stream.getViewerCount() - 1));
                streamRepository.save(stream);
            })
        );
    }

    /** Called from the LiveKit webhook when a room closes (everyone left). */
    @Transactional
    public void onRoomFinished(String roomName) {
        parseStreamId(roomName).ifPresent(id ->
            streamRepository.findById(id).ifPresent(stream -> {
                stream.setViewerCount(0);
                streamRepository.save(stream);
            })
        );
    }

    private java.util.Optional<Long> parseStreamId(String roomName) {
        if (roomName == null || !roomName.startsWith(ROOM_PREFIX)) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Long.parseLong(roomName.substring(ROOM_PREFIX.length())));
        } catch (NumberFormatException e) {
            log.warn("could not parse stream id from livekit room name: {}", roomName);
            return java.util.Optional.empty();
        }
    }
}
