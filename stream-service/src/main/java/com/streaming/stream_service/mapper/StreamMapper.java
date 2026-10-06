package com.streaming.stream_service.mapper;

import org.springframework.stereotype.Component;

import com.streaming.stream_service.dto.CreateStreamRequest;
import com.streaming.stream_service.dto.StreamPublicResponse;
import com.streaming.stream_service.dto.StreamResponse;
import com.streaming.stream_service.entity.Stream;
import com.streaming.stream_service.enums.StreamCategory;
import com.streaming.stream_service.enums.StreamStatus;

@Component
public class StreamMapper {

    public Stream toEntity(CreateStreamRequest request, Long userId) {
        Stream stream = new Stream();

        stream.setUserId(userId);
        stream.setTitle(request.title());
        stream.setDescription(request.description());
        stream.setCategory(request.category() != null ? request.category() : StreamCategory.JUST_CHATTING);
        stream.setStatus(StreamStatus.LIVE);
        stream.setViewerCount(0);

        return stream;
    }

    /** Full response including the stream key — only for the channel owner. */
    public StreamResponse toResponse(Stream stream) {
        return new StreamResponse(
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
    }

    /** Public response — never exposes the stream key. */
    public StreamPublicResponse toPublicResponse(Stream stream) {
        return new StreamPublicResponse(
            stream.getId(),
            stream.getUserId(),
            stream.getTitle(),
            stream.getDescription(),
            stream.getCategory(),
            stream.getStatus(),
            stream.getViewerCount(),
            stream.getCreatedAt()
        );
    }
}
