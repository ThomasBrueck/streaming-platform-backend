package com.streaming.user_service.exception;

public class UnauthorizedMethod extends RuntimeException {
    public UnauthorizedMethod(String message) {
        super(message);
    }
    
}
