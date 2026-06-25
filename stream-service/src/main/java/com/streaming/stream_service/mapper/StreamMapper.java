package com.streaming.stream_service.mapper;

import org.springframework.stereotype.Component;

import com.streaming.stream_service.dto.CreateStreamRequest;
import com.streaming.stream_service.dto.StreamResponse;
import com.streaming.stream_service.entity.Stream;

@Component
public class StreamMapper {
    
    public Stream toEntity(CreateStreamRequest request) {
        Stream stream = new Stream();

        stream.setUserId(request.userId());
        stream.setTitle(request.title());
        stream.setDescription(request.description());
        stream.setCategory(request.category());

        return stream;
    }

    public StreamResponse toResponse(Stream stream) {
        StreamResponse streamResponse = new StreamResponse(
            stream.getId(),
            stream.getUserId(),
            stream.getTitle(),
            stream.getDescription(),
            stream.getCategory(),
            stream.getStatus(),
            stream.getStreamKey(),
            stream.getViewerCount(),
            stream.getCreatedAt()
        );

        return streamResponse;
    }
}
