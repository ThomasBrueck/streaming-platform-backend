package com.streaming.stream_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streaming.stream_service.dto.CreateStreamRequest;
import com.streaming.stream_service.dto.LiveKitTokenResponse;
import com.streaming.stream_service.dto.StreamPublicResponse;
import com.streaming.stream_service.dto.StreamResponse;
import com.streaming.stream_service.dto.UpdateStreamRequest;
import com.streaming.stream_service.entity.Stream;
import com.streaming.stream_service.enums.StreamCategory;
import com.streaming.stream_service.enums.StreamStatus;
import com.streaming.stream_service.exception.DuplicateStreamException;
import com.streaming.stream_service.exception.StreamNotFoundException;
import com.streaming.stream_service.exception.UnauthorizedMethod;
import com.streaming.stream_service.mapper.StreamMapper;
import com.streaming.stream_service.repository.StreamRepository;

@ExtendWith(MockitoExtension.class)
class StreamServiceTest {

    @Mock
    private StreamRepository streamRepository;

    @Mock
    private LiveKitService liveKitService;

    private StreamService streamService;

    @BeforeEach
    void setUp() {
        streamService = new StreamService(streamRepository, new StreamMapper(), liveKitService);
    }

    private Stream sampleStream(Long id, Long ownerId) {
        Stream stream = new Stream();
        stream.setId(id);
        stream.setUserId(ownerId);
        stream.setTitle("My stream");
        stream.setCategory(StreamCategory.JUST_CHATTING);
        stream.setStatus(StreamStatus.LIVE);
        stream.setStreamKey("live_abc123");
        stream.setViewerCount(0);
        return stream;
    }

    @Test
    void createStream_persistsWithGeneratedStreamKey() {
        when(streamRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(streamRepository.save(any(Stream.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateStreamRequest request = new CreateStreamRequest("Title", "Desc", StreamCategory.GAMING);
        StreamResponse response = streamService.createStream(request, 1L);

        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.category()).isEqualTo(StreamCategory.GAMING);
        assertThat(response.streamKey()).startsWith("live_");
        assertThat(response.status()).isEqualTo(StreamStatus.LIVE);
    }

    @Test
    void createStream_defaultsCategoryWhenMissing() {
        when(streamRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(streamRepository.save(any(Stream.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateStreamRequest request = new CreateStreamRequest("Title", null, null);
        StreamResponse response = streamService.createStream(request, 1L);

        assertThat(response.category()).isEqualTo(StreamCategory.JUST_CHATTING);
    }

    @Test
    void createStream_rejectsWhenUserAlreadyHasAChannel() {
        Stream existing = sampleStream(1L, 1L);
        when(streamRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        CreateStreamRequest request = new CreateStreamRequest("Second channel", null, null);

        assertThatThrownBy(() -> streamService.createStream(request, 1L))
            .isInstanceOf(DuplicateStreamException.class);
    }

    @Test
    void updateStream_ownerCanEditTitleAndCategory() {
        Stream stream = sampleStream(1L, 42L);
        when(streamRepository.findById(1L)).thenReturn(Optional.of(stream));
        when(streamRepository.save(any(Stream.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateStreamRequest request = new UpdateStreamRequest("New title", "New description", StreamCategory.SOFTWARE);
        StreamResponse response = streamService.updateStream(1L, request, 42L);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.category()).isEqualTo(StreamCategory.SOFTWARE);
    }

    @Test
    void updateStream_nonOwnerIsRejected() {
        Stream stream = sampleStream(1L, 42L);
        when(streamRepository.findById(1L)).thenReturn(Optional.of(stream));

        UpdateStreamRequest request = new UpdateStreamRequest("New title", null, null);

        assertThatThrownBy(() -> streamService.updateStream(1L, request, 99L))
            .isInstanceOf(UnauthorizedMethod.class);
    }

    @Test
    void updateStreamStatusById_ownerCanUpdate() {
        Stream stream = sampleStream(1L, 42L);
        when(streamRepository.findById(1L)).thenReturn(Optional.of(stream));
        when(streamRepository.save(any(Stream.class))).thenAnswer(inv -> inv.getArgument(0));

        StreamResponse response = streamService.updateStreamStatusById(1L, StreamStatus.OFFLINE, 42L);

        assertThat(response.status()).isEqualTo(StreamStatus.OFFLINE);
        assertThat(response.viewerCount()).isZero();
    }

    @Test
    void updateStreamStatusById_nonOwnerIsRejected() {
        Stream stream = sampleStream(1L, 42L);
        when(streamRepository.findById(1L)).thenReturn(Optional.of(stream));

        assertThatThrownBy(() -> streamService.updateStreamStatusById(1L, StreamStatus.OFFLINE, 99L))
            .isInstanceOf(UnauthorizedMethod.class);
    }

    @Test
    void getStreamById_returnsPublicResponseWithoutStreamKey() {
        Stream stream = sampleStream(1L, 42L);
        when(streamRepository.findById(1L)).thenReturn(Optional.of(stream));

        StreamPublicResponse response = streamService.getStreamById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("My stream");
        // StreamPublicResponse must NOT expose the stream key
        assertThat(response).isInstanceOf(StreamPublicResponse.class);
    }

    @Test
    void getStreamById_notFoundThrows() {
        when(streamRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> streamService.getStreamById(1L))
            .isInstanceOf(StreamNotFoundException.class);
    }

    @Test
    void createLiveKitToken_grantsPublishOnlyToOwner() {
        Stream stream = sampleStream(1L, 42L);
        when(streamRepository.findById(1L)).thenReturn(Optional.of(stream));
        when(liveKitService.generateIdentity(42L)).thenReturn("user-42-abcd1234");
        when(liveKitService.getWsUrl()).thenReturn("ws://localhost:7880");
        when(liveKitService.createAccessToken(any(), any(), any(), any(Boolean.class))).thenReturn("signed-token");

        LiveKitTokenResponse response = streamService.createLiveKitToken(1L, 42L, "Owner");

        assertThat(response.canPublish()).isTrue();
        assertThat(response.roomName()).isEqualTo("stream_1");
        verify(liveKitService).createAccessToken("user-42-abcd1234", "Owner", "stream_1", true);
    }

    @Test
    void createLiveKitToken_viewerCannotPublish() {
        Stream stream = sampleStream(1L, 42L);
        when(streamRepository.findById(1L)).thenReturn(Optional.of(stream));
        when(liveKitService.generateIdentity(7L)).thenReturn("user-7-zzzz9999");
        when(liveKitService.getWsUrl()).thenReturn("ws://localhost:7880");

        LiveKitTokenResponse response = streamService.createLiveKitToken(1L, 7L, null);

        assertThat(response.canPublish()).isFalse();
        verify(liveKitService).createAccessToken(any(), any(), any(), org.mockito.ArgumentMatchers.eq(false));
    }

    @Test
    void onParticipantJoined_incrementsViewerCount() {
        Stream stream = sampleStream(5L, 1L);
        stream.setViewerCount(2);
        when(streamRepository.findById(5L)).thenReturn(Optional.of(stream));
        when(streamRepository.save(any(Stream.class))).thenAnswer(inv -> inv.getArgument(0));

        streamService.onParticipantJoined("stream_5");

        assertThat(stream.getViewerCount()).isEqualTo(3);
    }

    @Test
    void onParticipantLeft_neverGoesNegative() {
        Stream stream = sampleStream(5L, 1L);
        stream.setViewerCount(0);
        when(streamRepository.findById(5L)).thenReturn(Optional.of(stream));
        when(streamRepository.save(any(Stream.class))).thenAnswer(inv -> inv.getArgument(0));

        streamService.onParticipantLeft("stream_5");

        assertThat(stream.getViewerCount()).isZero();
    }
}
