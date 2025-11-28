package com.ausganslage.ausgangslageBackend.dto;

import com.ausganslage.ausgangslageBackend.enums.RoleName;

public class PlayerInfoDto {
    private Long playerId;
    private Long userId;
    private String username;
    private String avatarConfig;
    private Integer seatNumber;
    private Boolean isAlive;
    private Boolean revealedRole;
    private RoleName role;

    public PlayerInfoDto() {
    }

    public PlayerInfoDto(Long playerId, Long userId, String username, String avatarConfig, Integer seatNumber, Boolean isAlive, Boolean revealedRole, RoleName role) {
        this.playerId = playerId;
        this.userId = userId;
        this.username = username;
        this.avatarConfig = avatarConfig;
        this.seatNumber = seatNumber;
        this.isAlive = isAlive;
        this.revealedRole = revealedRole;
        this.role = role;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarConfig() {
        return avatarConfig;
    }

    public void setAvatarConfig(String avatarConfig) {
        this.avatarConfig = avatarConfig;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Boolean getIsAlive() {
        return isAlive;
    }

    public void setIsAlive(Boolean isAlive) {
        this.isAlive = isAlive;
    }

    public Boolean getRevealedRole() {
        return revealedRole;
    }

    public void setRevealedRole(Boolean revealedRole) {
        this.revealedRole = revealedRole;
    }

    public RoleName getRole() {
        return role;
    }

    public void setRole(RoleName role) {
        this.role = role;
    }
}

