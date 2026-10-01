package com.apex.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  public record ApiError(int status, String message, List<String> details, LocalDateTime timestamp) {
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
    return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(
      org.springframework.security.access.AccessDeniedException ex) {
    return build(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions", null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getDefaultMessage())
        .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
  }

  private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
    return ResponseEntity.status(status)
        .body(new ApiError(status.value(), message, details, LocalDateTime.now()));
  }
}
