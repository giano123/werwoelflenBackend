package com.ausganslage.ausgangslageBackend.model;

import com.ausganslage.ausgangslageBackend.enums.Faction;
import com.ausganslage.ausgangslageBackend.enums.GamePhase;
import com.ausganslage.ausgangslageBackend.enums.GameStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long lobbyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.STARTING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase currentPhase = GamePhase.NIGHT_WOLVES;

    @Column(nullable = false)
    private Integer dayNumber = 1;

    @Enumerated(EnumType.STRING)
    private Faction winnerFaction;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant finishedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Game() {
    }

    public Game(Long id, Long lobbyId, GameStatus status, GamePhase currentPhase, Integer dayNumber, Faction winnerFaction, Instant createdAt, Instant finishedAt) {
        this.id = id;
        this.lobbyId = lobbyId;
        this.status = status;
        this.currentPhase = currentPhase;
        this.dayNumber = dayNumber;
        this.winnerFaction = winnerFaction;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
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

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(GamePhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public Faction getWinnerFaction() {
        return winnerFaction;
    }

    public void setWinnerFaction(Faction winnerFaction) {
        this.winnerFaction = winnerFaction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

