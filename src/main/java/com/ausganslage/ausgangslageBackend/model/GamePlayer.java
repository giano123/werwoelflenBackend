package com.ausganslage.ausgangslageBackend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "game_players")
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long roleId;

    @Column(nullable = false)
    private Integer seatNumber;

    @Column(nullable = false)
    private Boolean isAlive = true;

    @Column(nullable = false)
    private Boolean revealedRole = false;

    @Column(length = 1000)
    private String stateFlagsJson = "{}";

    public GamePlayer() {
    }

    public GamePlayer(Long id, Long gameId, Long userId, Long roleId, Integer seatNumber, Boolean isAlive, Boolean revealedRole, String stateFlagsJson) {
        this.id = id;
        this.gameId = gameId;
        this.userId = userId;
        this.roleId = roleId;
        this.seatNumber = seatNumber;
        this.isAlive = isAlive;
        this.revealedRole = revealedRole;
        this.stateFlagsJson = stateFlagsJson;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
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

    public String getStateFlagsJson() {
        return stateFlagsJson;
    }

    public void setStateFlagsJson(String stateFlagsJson) {
        this.stateFlagsJson = stateFlagsJson;
    }
}

