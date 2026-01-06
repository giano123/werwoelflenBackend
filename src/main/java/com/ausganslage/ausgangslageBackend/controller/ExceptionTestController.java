package com.ausganslage.ausgangslageBackend.controller;

import com.ausganslage.ausgangslageBackend.exception.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-exceptions")
@Profile({"dev", "test"})
public class ExceptionTestController {

    @GetMapping("/resource-not-found")
    public void testResourceNotFound() {
        throw new ResourceNotFoundException("Game", 999L);
    }

    @GetMapping("/invalid-state")
    public void testInvalidState() {
        throw new InvalidGameStateException("Wrong phase", "NIGHT", "DAY");
    }

    @GetMapping("/unauthorized")
    public void testUnauthorized() {
        throw new UnauthorizedActionException("Not allowed", 1L, "TEST_ACTION");
    }

    @GetMapping("/duplicate")
    public void testDuplicate() {
        throw new DuplicateResourceException("User", "email", "test@test.com");
    }

    @GetMapping("/invalid-action")
    public void testInvalidAction() {
        throw new InvalidActionException("VOTE", "Player is dead");
    }
}

