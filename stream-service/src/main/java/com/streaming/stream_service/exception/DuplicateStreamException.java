package com.streaming.stream_service.exception;

/** Thrown when a user who already owns a channel tries to create another one. */
public class DuplicateStreamException extends RuntimeException {
    public DuplicateStreamException(String message) {
        super(message);
    }
}
