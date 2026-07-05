package com.streaming.user_service.event;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.streaming.user_service.service.UserService;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private UserService userService;

    public UserEventListener(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "auth-events")
    public void onCreatedUser(UserCreatedEvent event) {
        log.info("received event: user created - user_id: " + event.userId());
        userService.createUser(event);
    }
}
