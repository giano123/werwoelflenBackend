package com.ausganslage.ausgangslageBackend.dto;

import com.ausganslage.ausgangslageBackend.enums.RoleName;

public class InspectionResultDto {
    private Long playerId;
    private String username;
    private RoleName role;

    public InspectionResultDto() {
    }

    public InspectionResultDto(Long playerId, String username, RoleName role) {
        this.playerId = playerId;
        this.username = username;
        this.role = role;
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

    public RoleName getRole() {
        return role;
    }

    public void setRole(RoleName role) {
        this.role = role;
    }
}

