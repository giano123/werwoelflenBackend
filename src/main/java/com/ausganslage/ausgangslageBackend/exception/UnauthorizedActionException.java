package com.ausganslage.ausgangslageBackend.exception;

public class UnauthorizedActionException extends RuntimeException {

    private final Long userId;
    private final String action;

    public UnauthorizedActionException(String message, Long userId, String action) {
        super(message);
        this.userId = userId;
        this.action = action;
    }

    public UnauthorizedActionException(String message) {
        super(message);
        this.userId = null;
        this.action = null;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }
}

