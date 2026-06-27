package com.streaming.user_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {
    private static final String TOPIC = "user-events";

    private final KafkaTemplate<String, UserDeletedEvent> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, UserDeletedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publisherUserDeleted(Long userId) {
        kafkaTemplate.send(TOPIC, new UserDeletedEvent(userId));
    }
}
