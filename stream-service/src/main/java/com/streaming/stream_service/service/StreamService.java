package com.streaming.stream_service.service;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streaming.stream_service.dto.CreateStreamRequest;
import com.streaming.stream_service.dto.StreamResponse;
import com.streaming.stream_service.dto.UpdateStreamStatusRequest;
import com.streaming.stream_service.entity.Stream;
import com.streaming.stream_service.enums.StreamStatus;
import com.streaming.stream_service.exception.StreamNotFoundException;
import com.streaming.stream_service.mapper.StreamMapper;
import com.streaming.stream_service.repository.StreamRepository;

@Service
public class StreamService {
    
    private final StreamRepository streamRepository;
    private final StreamMapper streamMapper;

    public StreamService(StreamRepository streamRepository, StreamMapper streamMapper) {
        this.streamRepository = streamRepository;
        this.streamMapper = streamMapper;
    }

    @Transactional
    public StreamResponse createStream(CreateStreamRequest request) {
        Stream stream = streamMapper.toEntity(request);

        stream.setStatus(StreamStatus.OFFLINE);
        stream.setStreamKey(generateStreamKey());
        stream.setViewerCount(0);

        Stream saved = streamRepository.save(stream);
        return streamMapper.toResponse(saved);
    }

    private String generateStreamKey() {
        return "live_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional
    public StreamResponse updateStreamStatusById(Long id, StreamStatus status) {
        Stream stream = streamRepository.findById(id).orElseThrow(() -> new StreamNotFoundException("stream not found: " + id));

        stream.setStatus(status);
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
        System.out.println("deleted streams of user id: " + userId + ". Quantity: " + deleted);
    }






}
