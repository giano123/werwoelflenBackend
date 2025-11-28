package com.ausganslage.ausgangslageBackend.dto;

public class LobbyMemberDto {
    private Long id;
    private Long userId;
    private String username;
    private String avatarConfig;
    private Boolean isHost;
    private Boolean isReady;

    public LobbyMemberDto() {
    }

    public LobbyMemberDto(Long id, Long userId, String username, String avatarConfig, Boolean isHost, Boolean isReady) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.avatarConfig = avatarConfig;
        this.isHost = isHost;
        this.isReady = isReady;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getIsHost() {
        return isHost;
    }

    public void setIsHost(Boolean isHost) {
        this.isHost = isHost;
    }

    public Boolean getIsReady() {
        return isReady;
    }

    public void setIsReady(Boolean isReady) {
        this.isReady = isReady;
    }
}

