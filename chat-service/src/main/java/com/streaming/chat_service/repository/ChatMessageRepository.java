package com.streaming.chat_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.streaming.chat_service.entity.ChatMessageEntity;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findTop50ByStreamIdOrderByCreatedAtDesc(Long streamId);
}
