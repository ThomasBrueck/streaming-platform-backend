package com.streaming.stream_service.exception;

public class StreamNotFoundException extends RuntimeException {
    public StreamNotFoundException(String message) {
        super(message);
    }
}
