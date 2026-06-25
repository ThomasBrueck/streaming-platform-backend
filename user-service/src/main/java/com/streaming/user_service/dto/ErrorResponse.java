package com.streaming.user_service.dto;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class ErrorResponse {
    String message;
    int status;
    LocalDateTime timestamp;

    public ErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
        timestamp = LocalDateTime.now();
    }
}
