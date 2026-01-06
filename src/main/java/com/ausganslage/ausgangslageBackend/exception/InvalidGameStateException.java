package com.ausganslage.ausgangslageBackend.exception;

public class InvalidGameStateException extends RuntimeException {

    private final String currentState;
    private final String expectedState;

    public InvalidGameStateException(String message, String currentState, String expectedState) {
        super(message);
        this.currentState = currentState;
        this.expectedState = expectedState;
    }

    public InvalidGameStateException(String message) {
        super(message);
        this.currentState = null;
        this.expectedState = null;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getExpectedState() {
        return expectedState;
    }
}

