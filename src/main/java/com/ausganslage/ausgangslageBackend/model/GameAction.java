package com.ausganslage.ausgangslageBackend.model;

import com.ausganslage.ausgangslageBackend.enums.ActionType;
import com.ausganslage.ausgangslageBackend.enums.GamePhase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "game_actions")
public class GameAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Integer dayNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase phase;

    private Long actorPlayerId;

    private Long targetPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Column(length = 1000)
    private String payloadJson = "{}";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public GameAction() {
    }

    public GameAction(Long id, Long gameId, Integer dayNumber, GamePhase phase, Long actorPlayerId, Long targetPlayerId, ActionType actionType, String payloadJson, Instant createdAt) {
        this.id = id;
        this.gameId = gameId;
        this.dayNumber = dayNumber;
        this.phase = phase;
        this.actorPlayerId = actorPlayerId;
        this.targetPlayerId = targetPlayerId;
        this.actionType = actionType;
        this.payloadJson = payloadJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public Long getActorPlayerId() {
        return actorPlayerId;
    }

    public void setActorPlayerId(Long actorPlayerId) {
        this.actorPlayerId = actorPlayerId;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

