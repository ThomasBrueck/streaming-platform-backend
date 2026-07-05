package com.streaming.auth_service.exception;


public class DuplicatedResourceException extends RuntimeException {

    public DuplicatedResourceException(String message) {
        super(message);
    }
    
}
