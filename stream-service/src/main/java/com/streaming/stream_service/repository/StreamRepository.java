package com.streaming.stream_service.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.streaming.stream_service.entity.Stream;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    Optional<Stream> findByUserId(Long userId);

    Optional<Stream> findByStreamKey(String streamKey);

    boolean existsByStreamKey(String streamKey);   
}
