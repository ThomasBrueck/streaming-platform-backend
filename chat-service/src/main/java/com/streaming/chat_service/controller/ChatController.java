package com.streaming.chat_service.controller;

import java.time.Instant;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.streaming.chat_service.dto.ChatMessage;
import com.streaming.chat_service.event.ChatMessageProducer;

@Controller
public class ChatController {
    private final ChatMessageProducer chatProducer;

    public ChatController(ChatMessageProducer chatProducer) {
        this.chatProducer = chatProducer;
    }

    @MessageMapping("/chat/{streamId}")
    public void sendMessage(@DestinationVariable String streamId, @Payload ChatMessage message) {
        // Timestamp is always stamped here, server-side — never trust a client-supplied one.
        ChatMessage stamped = new ChatMessage(streamId, message.userId(), message.username(), message.content(), Instant.now().toString());
        chatProducer.sendMessage(streamId, stamped);
    }
}
