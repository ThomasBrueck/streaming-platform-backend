package com.streaming.auth_service.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;

public class DuplicatedResourceException extends RuntimeException {

    public DuplicatedResourceException(String message) {
        super(message);
    }
    
}
