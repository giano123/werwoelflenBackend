package com.ausganslage.ausgangslageBackend.exception;

public class InvalidActionException extends RuntimeException {

    private final String action;
    private final String reason;

    public InvalidActionException(String action, String reason) {
        super(String.format("Cannot perform action '%s': %s", action, reason));
        this.action = action;
        this.reason = reason;
    }

    public InvalidActionException(String message) {
        super(message);
        this.action = null;
        this.reason = null;
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }
}

