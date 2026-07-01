package com.streaming.chat_service.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.streaming.chat_service.dto.ChatMessage;

@Service
public class ChatMessageListener {
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-service")
    public void consume(ChatMessage message) {
        String destination = "/topic/stream/" + message.streamId();
        messagingTemplate.convertAndSend(destination, message);
    }
}