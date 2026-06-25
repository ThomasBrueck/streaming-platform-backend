package com.streaming.stream_service.dto;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private String message;
    private int status;
    private LocalDateTime createdAt;

    public ErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
        createdAt = LocalDateTime.now();
    }
}
