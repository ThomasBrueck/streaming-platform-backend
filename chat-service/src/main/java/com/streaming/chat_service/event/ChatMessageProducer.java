package com.streaming.chat_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.streaming.chat_service.dto.ChatMessage;

@Service
public class ChatMessageProducer {
    private static final String TOPIC = "chat-messages";

    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

    public ChatMessageProducer(KafkaTemplate<String, ChatMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String streamId, ChatMessage message) {
        kafkaTemplate.send(TOPIC, streamId, message);
    }
}
