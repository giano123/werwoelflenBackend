package com.ausganslage.ausgangslageBackend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lobby_members")
public class LobbyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long lobbyId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Boolean isHost = false;

    @Column(nullable = false)
    private Boolean isReady = false;

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();

    public LobbyMember() {
    }

    public LobbyMember(Long id, Long lobbyId, Long userId, Boolean isHost, Boolean isReady, Instant joinedAt) {
        this.id = id;
        this.lobbyId = lobbyId;
        this.userId = userId;
        this.isHost = isHost;
        this.isReady = isReady;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}

