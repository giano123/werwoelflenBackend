package com.ausganslage.ausgangslageBackend.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    void testResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Game", 123L);
        assertEquals("Game with identifier '123' not found", ex.getMessage());
        assertEquals("Game", ex.getResourceType());
        assertEquals(123L, ex.getResourceId());
        System.out.println("✓ ResourceNotFoundException works!");
    }

    @Test
    void testInvalidGameStateException() {
        InvalidGameStateException ex = new InvalidGameStateException("Wrong phase", "NIGHT", "DAY");
        assertEquals("Wrong phase", ex.getMessage());
        assertEquals("NIGHT", ex.getCurrentState());
        assertEquals("DAY", ex.getExpectedState());
        System.out.println("✓ InvalidGameStateException works!");
    }

    @Test
    void testUnauthorizedActionException() {
        UnauthorizedActionException ex = new UnauthorizedActionException("Not allowed", 42L, "VOTE");
        assertEquals("Not allowed", ex.getMessage());
        assertEquals(42L, ex.getUserId());
        assertEquals("VOTE", ex.getAction());
        System.out.println("✓ UnauthorizedActionException works!");
    }

    @Test
    void testDuplicateResourceException() {
        DuplicateResourceException ex = new DuplicateResourceException("User", "email", "test@test.com");
        assertEquals("User with email 'test@test.com' already exists", ex.getMessage());
        assertEquals("User", ex.getResourceType());
        assertEquals("email", ex.getDuplicateField());
        assertEquals("test@test.com", ex.getDuplicateValue());
        System.out.println("✓ DuplicateResourceException works!");
    }

    @Test
    void testInvalidActionException() {
        InvalidActionException ex = new InvalidActionException("VOTE", "Player is dead");
        assertEquals("Cannot perform action 'VOTE': Player is dead", ex.getMessage());
        assertEquals("VOTE", ex.getAction());
        assertEquals("Player is dead", ex.getReason());
        System.out.println("✓ InvalidActionException works!");
    }
}

