package com.ausganslage.ausgangslageBackend.dto;

import com.ausganslage.ausgangslageBackend.enums.Faction;
import com.ausganslage.ausgangslageBackend.enums.GamePhase;
import com.ausganslage.ausgangslageBackend.enums.GameStatus;
import com.ausganslage.ausgangslageBackend.enums.RoleName;
import java.util.List;
import java.util.Map;

public class GameStateDto {
    private Long gameId;
    private GameStatus status;
    private GamePhase currentPhase;
    private Integer dayNumber;
    private Faction winnerFaction;

    private RoleName ownRole;
    private Faction ownFaction;
    private Boolean isAlive;
    private Map<String, Object> ownStateFlags;

    private List<PlayerInfoDto> players;
    private List<String> availableActions;
    private String phaseDescription;

    private WolfVictimDto wolfVictim;
    private InspectionResultDto lastInspection;

    public GameStateDto() {
    }

    public GameStateDto(Long gameId, GameStatus status, GamePhase currentPhase, Integer dayNumber, Faction winnerFaction, RoleName ownRole, Faction ownFaction, Boolean isAlive, Map<String, Object> ownStateFlags, List<PlayerInfoDto> players, List<String> availableActions, String phaseDescription, WolfVictimDto wolfVictim, InspectionResultDto lastInspection) {
        this.gameId = gameId;
        this.status = status;
        this.currentPhase = currentPhase;
        this.dayNumber = dayNumber;
        this.winnerFaction = winnerFaction;
        this.ownRole = ownRole;
        this.ownFaction = ownFaction;
        this.isAlive = isAlive;
        this.ownStateFlags = ownStateFlags;
        this.players = players;
        this.availableActions = availableActions;
        this.phaseDescription = phaseDescription;
        this.wolfVictim = wolfVictim;
        this.lastInspection = lastInspection;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
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

    public RoleName getOwnRole() {
        return ownRole;
    }

    public void setOwnRole(RoleName ownRole) {
        this.ownRole = ownRole;
    }

    public Faction getOwnFaction() {
        return ownFaction;
    }

    public void setOwnFaction(Faction ownFaction) {
        this.ownFaction = ownFaction;
    }

    public Boolean getIsAlive() {
        return isAlive;
    }

    public void setIsAlive(Boolean isAlive) {
        this.isAlive = isAlive;
    }

    public Map<String, Object> getOwnStateFlags() {
        return ownStateFlags;
    }

    public void setOwnStateFlags(Map<String, Object> ownStateFlags) {
        this.ownStateFlags = ownStateFlags;
    }

    public List<PlayerInfoDto> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfoDto> players) {
        this.players = players;
    }

    public List<String> getAvailableActions() {
        return availableActions;
    }

    public void setAvailableActions(List<String> availableActions) {
        this.availableActions = availableActions;
    }

    public String getPhaseDescription() {
        return phaseDescription;
    }

    public void setPhaseDescription(String phaseDescription) {
        this.phaseDescription = phaseDescription;
    }

    public WolfVictimDto getWolfVictim() {
        return wolfVictim;
    }

    public void setWolfVictim(WolfVictimDto wolfVictim) {
        this.wolfVictim = wolfVictim;
    }

    public InspectionResultDto getLastInspection() {
        return lastInspection;
    }

    public void setLastInspection(InspectionResultDto lastInspection) {
        this.lastInspection = lastInspection;
    }
}
