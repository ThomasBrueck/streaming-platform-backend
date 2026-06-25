package com.streaming.stream_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.streaming.stream_service.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalHandlerException {

    @ExceptionHandler(StreamNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStreamNotFound(StreamNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
}
