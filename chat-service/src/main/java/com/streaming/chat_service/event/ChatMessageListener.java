package com.streaming.chat_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.streaming.chat_service.dto.ChatMessage;
import com.streaming.chat_service.entity.ChatMessageEntity;
import com.streaming.chat_service.repository.ChatMessageRepository;

@Service
public class ChatMessageListener {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageListener(SimpMessagingTemplate messagingTemplate, ChatMessageRepository chatMessageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-service")
    public void consume(ChatMessage message) {
        persist(message);

        String destination = "/topic/stream/" + message.streamId();
        messagingTemplate.convertAndSend(destination, message);
    }

    private void persist(ChatMessage message) {
        try {
            ChatMessageEntity entity = new ChatMessageEntity();
            entity.setStreamId(Long.parseLong(message.streamId()));
            entity.setUserId(message.userId());
            entity.setUsername(message.username());
            entity.setContent(message.content());
            chatMessageRepository.save(entity);
        } catch (NumberFormatException e) {
            log.warn("could not persist chat message, invalid streamId: {}", message.streamId());
        }
    }
}
