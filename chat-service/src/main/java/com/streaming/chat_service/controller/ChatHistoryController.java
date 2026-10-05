package com.streaming.chat_service.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streaming.chat_service.dto.ChatMessage;
import com.streaming.chat_service.entity.ChatMessageEntity;
import com.streaming.chat_service.repository.ChatMessageRepository;

/** Lets a viewer joining a stream late load the recent chat history over REST. */
@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatMessageRepository chatMessageRepository;

    public ChatHistoryController(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/{streamId}/history")
    public List<ChatMessage> getHistory(@PathVariable Long streamId) {
        List<ChatMessageEntity> messages = chatMessageRepository.findTop50ByStreamIdOrderByCreatedAtDesc(streamId);
        Collections.reverse(messages); // oldest first, chronological order for the UI

        return messages.stream()
            .map(m -> new ChatMessage(
                String.valueOf(m.getStreamId()),
                m.getUserId(),
                m.getUsername(),
                m.getContent(),
                m.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant().toString()
            ))
            .toList();
    }
}
