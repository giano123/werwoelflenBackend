package com.ausganslage.ausgangslageBackend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");

    public static void logUserRegistration(String username, Long userId) {
        AUDIT_LOG.info("USER_REGISTRATION | Username: {} | UserId: {}", username, userId);
    }

    public static void logUserLogin(String username, Long userId, String sessionToken) {
        AUDIT_LOG.info("USER_LOGIN | Username: {} | UserId: {} | SessionToken: {}",
            username, userId, maskToken(sessionToken));
    }

    public static void logUserLogout(String username, Long userId) {
        AUDIT_LOG.info("USER_LOGOUT | Username: {} | UserId: {}", username, userId);
    }

    public static void logLobbyCreated(Long lobbyId, String lobbyCode, Long creatorId, String creatorName) {
        AUDIT_LOG.info("LOBBY_CREATED | LobbyId: {} | LobbyCode: {} | CreatorId: {} | CreatorName: {}",
            lobbyId, lobbyCode, creatorId, creatorName);
    }

    public static void logPlayerJoinedLobby(Long lobbyId, String lobbyCode, Long userId, String username) {
        AUDIT_LOG.info("PLAYER_JOINED_LOBBY | LobbyId: {} | LobbyCode: {} | UserId: {} | Username: {}",
            lobbyId, lobbyCode, userId, username);
    }

    public static void logPlayerLeftLobby(Long lobbyId, String lobbyCode, Long userId, String username) {
        AUDIT_LOG.info("PLAYER_LEFT_LOBBY | LobbyId: {} | LobbyCode: {} | UserId: {} | Username: {}",
            lobbyId, lobbyCode, userId, username);
    }

    public static void logGameStarted(Long gameId, Long lobbyId, int playerCount, Long startedBy) {
        AUDIT_LOG.info("GAME_STARTED | GameId: {} | LobbyId: {} | PlayerCount: {} | StartedBy: {}",
            gameId, lobbyId, playerCount, startedBy);
    }

    public static void logGameEnded(Long gameId, String winningFaction, int duration) {
        AUDIT_LOG.info("GAME_ENDED | GameId: {} | WinningFaction: {} | DurationSeconds: {}",
            gameId, winningFaction, duration);
    }

    public static void logRoleAssignment(Long gameId, Long playerId, String username, String role) {
        AUDIT_LOG.info("ROLE_ASSIGNED | GameId: {} | PlayerId: {} | Username: {} | Role: {}",
            gameId, playerId, username, role);
    }

    public static void logPlayerAction(Long gameId, Long actorId, String actorName, String actionType,
                                      Long targetId, String targetName, String phase) {
        AUDIT_LOG.info("PLAYER_ACTION | GameId: {} | ActorId: {} | ActorName: {} | ActionType: {} | TargetId: {} | TargetName: {} | Phase: {}",
            gameId, actorId, actorName, actionType, targetId, targetName, phase);
    }

    public static void logPlayerDeath(Long gameId, Long playerId, String playerName, String cause, int dayNumber) {
        AUDIT_LOG.info("PLAYER_DEATH | GameId: {} | PlayerId: {} | PlayerName: {} | Cause: {} | DayNumber: {}",
            gameId, playerId, playerName, cause, dayNumber);
    }

    public static void logPhaseChange(Long gameId, String fromPhase, String toPhase, int dayNumber) {
        AUDIT_LOG.info("PHASE_CHANGE | GameId: {} | FromPhase: {} | ToPhase: {} | DayNumber: {}",
            gameId, fromPhase, toPhase, dayNumber);
    }

    public static void logVoteResult(Long gameId, Long votedPlayerId, String votedPlayerName,
                                     int votesReceived, int totalVotes, String phase) {
        AUDIT_LOG.info("VOTE_RESULT | GameId: {} | VotedPlayerId: {} | VotedPlayerName: {} | VotesReceived: {} | TotalVotes: {} | Phase: {}",
            gameId, votedPlayerId, votedPlayerName, votesReceived, totalVotes, phase);
    }

    public static void logChatMessage(Long gameId, Long senderId, String senderName, String channel, int messageLength) {
        AUDIT_LOG.info("CHAT_MESSAGE | GameId: {} | SenderId: {} | SenderName: {} | Channel: {} | MessageLength: {}",
            gameId, senderId, senderName, channel, messageLength);
    }

    public static void logAuthenticationFailure(String username, String reason) {
        AUDIT_LOG.warn("AUTHENTICATION_FAILURE | Username: {} | Reason: {}", username, reason);
    }

    public static void logUnauthorizedAccess(Long userId, String username, String attemptedAction) {
        AUDIT_LOG.warn("UNAUTHORIZED_ACCESS | UserId: {} | Username: {} | AttemptedAction: {}",
            userId, username, attemptedAction);
    }

    public static void logDataIntegrityIssue(String issue, String details) {
        AUDIT_LOG.error("DATA_INTEGRITY_ISSUE | Issue: {} | Details: {}", issue, details);
    }

    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }
}

