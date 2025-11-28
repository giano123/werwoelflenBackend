package com.ausganslage.ausgangslageBackend.dto;

import com.ausganslage.ausgangslageBackend.enums.LobbyStatus;
import java.util.List;

public class LobbyStateDto {
    private Long id;
    private String lobbyCode;
    private Long hostUserId;
    private Integer maxPlayers;
    private LobbyStatus status;
    private String settingsJson;
    private List<LobbyMemberDto> members;

    public LobbyStateDto() {
    }

    public LobbyStateDto(Long id, String lobbyCode, Long hostUserId, Integer maxPlayers, LobbyStatus status, String settingsJson, List<LobbyMemberDto> members) {
        this.id = id;
        this.lobbyCode = lobbyCode;
        this.hostUserId = hostUserId;
        this.maxPlayers = maxPlayers;
        this.status = status;
        this.settingsJson = settingsJson;
        this.members = members;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLobbyCode() {
        return lobbyCode;
    }

    public void setLobbyCode(String lobbyCode) {
        this.lobbyCode = lobbyCode;
    }

    public Long getHostUserId() {
        return hostUserId;
    }

    public void setHostUserId(Long hostUserId) {
        this.hostUserId = hostUserId;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public LobbyStatus getStatus() {
        return status;
    }

    public void setStatus(LobbyStatus status) {
        this.status = status;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }

    public List<LobbyMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<LobbyMemberDto> members) {
        this.members = members;
    }
}

