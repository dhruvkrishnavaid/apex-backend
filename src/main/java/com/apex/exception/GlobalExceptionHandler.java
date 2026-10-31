package com.apex.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  public record ApiError(
      int status,
      String message,
      List<String> details,
      LocalDateTime timestamp) {
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

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
    return build(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions", null);
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  @ExceptionHandler({ IllegalArgumentException.class, PropertyReferenceException.class })
  public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getField() + ": " + f.getDefaultMessage())
        .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
    List<String> details = ex.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
        .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
    return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", null);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", null);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
    return build(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'", null);
  }

  @ExceptionHandler({ DataIntegrityViolationException.class, OptimisticLockingFailureException.class })
  public ResponseEntity<ApiError> handleConflict(Exception ex) {
    log.warn("Data conflict: {}", ex.getMessage());
    return build(HttpStatus.CONFLICT,
        "The request conflicts with the current state of the data. Reload and retry.", null);
  }

  /** Framework exceptions that already carry a status (404 for unknown routes, 405, 415...). */
  @ExceptionHandler({ ErrorResponseException.class, NoResourceFoundException.class,
      HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class })
  public ResponseEntity<ApiError> handleErrorResponse(Exception ex) {
    ErrorResponse response = (ErrorResponse) ex;
    HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
    return build(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR,
        response.getBody().getDetail(), null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneralException(Exception ex) {
    log.error("Unhandled exception", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
  }

  private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
    return ResponseEntity.status(status)
        .body(new ApiError(status.value(), message, details, LocalDateTime.now()));
  }
}
