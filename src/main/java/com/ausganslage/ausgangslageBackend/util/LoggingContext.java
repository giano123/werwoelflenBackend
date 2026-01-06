package com.ausganslage.ausgangslageBackend.util;

import org.slf4j.MDC;

public class LoggingContext {

    private static final String USER_ID = "userId";
    private static final String USERNAME = "username";
    private static final String GAME_ID = "gameId";
    private static final String LOBBY_ID = "lobbyId";
    private static final String ACTION = "action";
    private static final String SESSION_TOKEN = "sessionToken";
    private static final String IP_ADDRESS = "ipAddress";

    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
    }

    public static void setUsername(String username) {
        if (username != null) {
            MDC.put(USERNAME, username);
        }
    }

    public static void setGameId(Long gameId) {
        if (gameId != null) {
            MDC.put(GAME_ID, gameId.toString());
        }
    }

    public static void setLobbyId(Long lobbyId) {
        if (lobbyId != null) {
            MDC.put(LOBBY_ID, lobbyId.toString());
        }
    }

    public static void setAction(String action) {
        if (action != null) {
            MDC.put(ACTION, action);
        }
    }

    public static void setSessionToken(String token) {
        if (token != null) {
            String maskedToken = token.length() > 8 ? token.substring(0, 8) + "..." : token;
            MDC.put(SESSION_TOKEN, maskedToken);
        }
    }

    public static void setIpAddress(String ipAddress) {
        if (ipAddress != null) {
            MDC.put(IP_ADDRESS, ipAddress);
        }
    }

    public static void clear() {
        MDC.clear();
    }

    public static void clearUserId() {
        MDC.remove(USER_ID);
    }

    public static void clearGameId() {
        MDC.remove(GAME_ID);
    }

    public static void clearLobbyId() {
        MDC.remove(LOBBY_ID);
    }

    public static void clearAction() {
        MDC.remove(ACTION);
    }
}

