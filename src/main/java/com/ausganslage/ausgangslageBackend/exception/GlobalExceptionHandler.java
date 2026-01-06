package com.ausganslage.ausgangslageBackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException e) {
        logger.warn("Resource not found: {}", e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("resourceType", e.getResourceType());
        if (e.getResourceId() != null) {
            error.put("resourceId", e.getResourceId());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidGameStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidGameStateException(InvalidGameStateException e) {
        logger.warn("Invalid game state: {}", e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        if (e.getCurrentState() != null) {
            error.put("currentState", e.getCurrentState());
        }
        if (e.getExpectedState() != null) {
            error.put("expectedState", e.getExpectedState());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedActionException(UnauthorizedActionException e) {
        logger.warn("Unauthorized action: {}", e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        if (e.getUserId() != null) {
            error.put("userId", e.getUserId());
        }
        if (e.getAction() != null) {
            error.put("action", e.getAction());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResourceException(DuplicateResourceException e) {
        logger.warn("Duplicate resource: {}", e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        if (e.getResourceType() != null) {
            error.put("resourceType", e.getResourceType());
        }
        if (e.getDuplicateField() != null) {
            error.put("field", e.getDuplicateField());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidActionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidActionException(InvalidActionException e) {
        logger.warn("Invalid action: {}", e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        if (e.getAction() != null) {
            error.put("action", e.getAction());
        }
        if (e.getReason() != null) {
            error.put("reason", e.getReason());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("IllegalArgumentException handled: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException e) {
        logger.warn("IllegalStateException handled: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
        logger.error("Unexpected exception occurred: {}", e.getMessage(), e);
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
