package com.ausganslage.ausgangslageBackend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Game", 123L);
        ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Game", response.getBody().get("resourceType"));
        assertEquals(123L, response.getBody().get("resourceId"));
    }

    @Test
    void testInvalidGameStateException() {
        InvalidGameStateException ex = new InvalidGameStateException("Wrong phase", "NIGHT", "DAY");
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidGameStateException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("NIGHT", response.getBody().get("currentState"));
        assertEquals("DAY", response.getBody().get("expectedState"));
    }

    @Test
    void testUnauthorizedActionException() {
        UnauthorizedActionException ex = new UnauthorizedActionException("Not allowed", 42L, "VOTE");
        ResponseEntity<Map<String, Object>> response = handler.handleUnauthorizedActionException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(42L, response.getBody().get("userId"));
        assertEquals("VOTE", response.getBody().get("action"));
    }

    @Test
    void testDuplicateResourceException() {
        DuplicateResourceException ex = new DuplicateResourceException("User", "email", "test@test.com");
        ResponseEntity<Map<String, Object>> response = handler.handleDuplicateResourceException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("User", response.getBody().get("resourceType"));
        assertEquals("email", response.getBody().get("field"));
    }

    @Test
    void testInvalidActionException() {
        InvalidActionException ex = new InvalidActionException("VOTE", "Player is dead");
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidActionException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VOTE", response.getBody().get("action"));
        assertEquals("Player is dead", response.getBody().get("reason"));
    }
}

