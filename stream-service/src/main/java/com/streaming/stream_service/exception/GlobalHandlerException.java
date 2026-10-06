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

  @ExceptionHandler(UnauthorizedMethod.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedMethod(UnauthorizedMethod ex) {
    ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED.value());
    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(DuplicateStreamException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateStream(DuplicateStreamException ex) {
    ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

}
