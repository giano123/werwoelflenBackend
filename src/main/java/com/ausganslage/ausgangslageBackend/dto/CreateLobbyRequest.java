package com.ausganslage.ausgangslageBackend.dto;

public class CreateLobbyRequest {
    private Integer maxPlayers = 12;
    private String settingsJson = "{}";

    public CreateLobbyRequest() {
    }

    public CreateLobbyRequest(Integer maxPlayers, String settingsJson) {
        this.maxPlayers = maxPlayers;
        this.settingsJson = settingsJson;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }
}

