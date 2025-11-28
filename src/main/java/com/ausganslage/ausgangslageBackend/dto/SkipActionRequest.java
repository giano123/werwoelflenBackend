package com.ausganslage.ausgangslageBackend.dto;

public class SkipActionRequest {
    private boolean skip = true;

    public SkipActionRequest() {
    }

    public SkipActionRequest(boolean skip) {
        this.skip = skip;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }
}

