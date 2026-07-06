package com.streaming.auth_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.streaming.auth_service.service.AuthService;

@Component
public class AuthEventListener {
    private static final Logger log = LoggerFactory.getLogger(AuthEventListener.class);

    private AuthService authService;

    public AuthEventListener(AuthService authService) {
        this.authService = authService;
    }

    @KafkaListener(topics = "user-events")
    public void onDeletedUser(UserDeletedEvent event) {
        log.info("received event: user deleted - user id: " + event.userId());
        authService.deleteUser(event.userId());
    }
    
}
