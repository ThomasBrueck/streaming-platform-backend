package com.streaming.stream_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.streaming.stream_service.dto.ErrorResponse;

import jakarta.ws.rs.core.Response;

@RestControllerAdvice
public class GlobalHandlerException {

  @ExceptionHandler(StreamNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleStreamNotFound(StreamNotFoundException ex) {
    ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(UnauthorizedMethod.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedMethod(UnauthorizedMethod ex) {
    ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED.value());
    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

}
