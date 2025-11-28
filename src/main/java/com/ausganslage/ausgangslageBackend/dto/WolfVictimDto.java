package com.ausganslage.ausgangslageBackend.dto;

public class WolfVictimDto {
    private Long playerId;
    private String username;

    public WolfVictimDto() {
    }

    public WolfVictimDto(Long playerId, String username) {
        this.playerId = playerId;
        this.username = username;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

