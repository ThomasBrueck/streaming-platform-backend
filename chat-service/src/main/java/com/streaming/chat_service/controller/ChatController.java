package com.streaming.chat_service.controller;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.streaming.chat_service.dto.ChatMessage;
import com.streaming.chat_service.dto.ChatMessageRequest;
import com.streaming.chat_service.event.ChatMessageProducer;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatMessageProducer chatProducer;

    public ChatController(ChatMessageProducer chatProducer) {
        this.chatProducer = chatProducer;
    }

    /**
     * Receives a chat message from a STOMP client.
     *
     * Identity is read from the WebSocket session (set by
     * {@link com.streaming.chat_service.config.StompAuthInterceptor StompAuthInterceptor}
     * during CONNECT) — the client payload only carries the message content.
     */
    @MessageMapping("/chat/{streamId}")
    public void sendMessage(
            @DestinationVariable String streamId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> session = headerAccessor.getSessionAttributes();
        String userId = (session != null) ? (String) session.get("userId") : null;
        String username = (session != null) ? (String) session.get("username") : null;

        if (userId == null || username == null) {
            log.warn("chat message rejected: unauthenticated session");
            return;
        }

        String content = request.content();
        if (content == null || content.isBlank() || content.length() > 500) {
            return;
        }

        ChatMessage stamped = new ChatMessage(
                streamId, userId, username, content, Instant.now().toString());
        chatProducer.sendMessage(streamId, stamped);
    }
}
