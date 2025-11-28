package com.ausganslage.ausgangslageBackend.dto;

public class VoteActionRequest {
    private Long targetPlayerId;

    public VoteActionRequest() {
    }

    public VoteActionRequest(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }
}

