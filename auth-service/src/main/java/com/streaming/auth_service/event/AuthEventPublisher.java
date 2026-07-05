package com.streaming.auth_service.event;


import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthEventPublisher {
    private static final String TOPIC = "auth-events";

    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    public AuthEventPublisher(KafkaTemplate<String, UserCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void userCreated(UserCreatedEvent user) {
        kafkaTemplate.send(TOPIC, user);
    }
    
}
