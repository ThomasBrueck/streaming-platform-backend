package com.streaming.stream_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.streaming.stream_service.service.StreamService;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final StreamService streamService;

    public UserEventListener(StreamService streamService) {
        this.streamService = streamService;
    }

    @KafkaListener(topics = "user-events")
    public void onUserDeleted(UserDeletedEvent event) {
        log.info("received event: deleted user userId={}", event.userId());
        streamService.deleteStreamsByUserId(event.userId());
    }


}
