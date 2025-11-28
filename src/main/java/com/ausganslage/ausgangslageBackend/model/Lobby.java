package com.ausganslage.ausgangslageBackend.model;

import com.ausganslage.ausgangslageBackend.enums.LobbyStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lobbies")
public class Lobby {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String lobbyCode;

    @Column(nullable = false)
    private Long hostUserId;

    @Column(nullable = false)
    private Integer maxPlayers = 12;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LobbyStatus status = LobbyStatus.OPEN;

    @Column(length = 2000)
    private String settingsJson = "{}";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Lobby() {
    }

    public Lobby(Long id, String lobbyCode, Long hostUserId, Integer maxPlayers, LobbyStatus status, String settingsJson, Instant createdAt) {
        this.id = id;
        this.lobbyCode = lobbyCode;
        this.hostUserId = hostUserId;
        this.maxPlayers = maxPlayers;
        this.status = status;
        this.settingsJson = settingsJson;
        this.createdAt = createdAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

