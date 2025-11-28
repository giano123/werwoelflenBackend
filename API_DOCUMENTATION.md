# Werwölfeln Backend API Documentation

## Overview
Complete backend implementation for the Werwölfeln (Werewolf/Mafia) digital game using Spring Boot, JPA/Hibernate, and H2 database.

## Technology Stack
- **Framework**: Spring Boot 3.5.6
- **Database**: H2 (In-Memory)
- **Security**: BCrypt password hashing, token-based authentication
- **Build Tool**: Maven

## Database Schema

### Core Entities
- **User**: Player accounts with authentication
- **Session**: Authentication tokens
- **Lobby**: Game lobbies with join codes
- **LobbyMember**: Players in lobbies
- **RoleTemplate**: Game role definitions (Werewolf, Villager, Seer, Witch, Hunter)
- **Game**: Active game instances
- **GamePlayer**: Players in active games with roles
- **GameAction**: All player actions and votes
- **ChatMessage**: In-game and lobby chat

## API Endpoints

### Authentication
```
POST /api/auth/register
Body: { username, email, password }
Response: { token, user: { id, username, email, avatarConfig } }

POST /api/auth/login
Body: { usernameOrEmail, password }
Response: { token, user: { id, username, email, avatarConfig } }

GET /api/auth/me
Headers: Authorization: Bearer {token}
Response: { id, username, email, avatarConfig }
```

### Lobby Management
```
POST /api/lobbies
Headers: Authorization: Bearer {token}
Body: { maxPlayers?, settingsJson? }
Response: LobbyStateDto

GET /api/lobbies/{code}/state
Response: { id, lobbyCode, hostUserId, maxPlayers, status, settingsJson, members[] }

POST /api/lobbies/{code}/join
Headers: Authorization: Bearer {token}
Response: LobbyStateDto

POST /api/lobbies/{code}/leave
Headers: Authorization: Bearer {token}

POST /api/lobbies/{code}/ready?ready=true
Headers: Authorization: Bearer {token}

POST /api/lobbies/{code}/start
Headers: Authorization: Bearer {token} (Host only)
Response: Game object
```

### Game Endpoints
```
GET /api/games/{gameId}/state
Headers: Authorization: Bearer {token}
Response: Player-specific game state with role, available actions, etc.

GET /api/games/lobby/{lobbyCode}
Response: Get game by lobby code

POST /api/games/{gameId}/actions/vote
Headers: Authorization: Bearer {token}
Body: { targetPlayerId }

POST /api/games/{gameId}/actions/power
Headers: Authorization: Bearer {token}
Body: { actionType, targetPlayerId }

GET /api/games/{gameId}/chat?since={timestamp}
Response: Array of chat messages

POST /api/games/{gameId}/chat
Headers: Authorization: Bearer {token}
Body: { content }

POST /api/games/{gameId}/transition-to-voting
Transitions from DAY_DISCUSSION to DAY_VOTING

GET /api/games/{gameId}/wolf-victim
Response: { playerId, username } (Witch only during NIGHT_WITCH)

GET /api/games/{gameId}/inspection-result
Response: { playerId, username, role } (Seer only)
```

## Game Flow

### Phase Progression
1. **NIGHT_WOLVES**: Werewolves vote to kill a villager
2. **NIGHT_SEER**: Seer investigates one player
3. **NIGHT_WITCH**: Witch can heal wolf victim or poison someone
4. **DAY_DISCUSSION**: All alive players discuss
5. **DAY_VOTING**: All alive players vote to lynch someone
6. Back to NIGHT_WOLVES (day number increments)

### Role Abilities
- **Werewolf**: Vote to kill during NIGHT_WOLVES phase
- **Villager**: No special powers, participates in day voting
- **Seer**: Inspect one player per night (NIGHT_SEER phase)
- **Witch**: One-time heal potion, one-time poison potion (NIGHT_WITCH phase)
- **Hunter**: When killed, can shoot another player as revenge

### Win Conditions
- **Village wins**: All werewolves eliminated
- **Werewolves win**: Werewolves equal or outnumber villagers

## Role Distribution (Auto-calculated)
- **Werewolves**: max(1, playerCount / 4)
- **Seer**: 1
- **Witch**: 1
- **Hunter**: 1
- **Villagers**: Remaining players

## Action Validation
All actions are validated for:
- Correct game phase
- Player role permissions
- Player alive status
- One-time ability tracking (Witch potions, Hunter shot)
- Target validity

## Chat Channels
- **LOBBY**: Pre-game lobby chat
- **DAY**: Day phase discussion (all alive players)
- **NIGHT_WOLVES**: Werewolf team chat (night phase)
- **SYSTEM**: Automated game event messages

## Setup & Running

1. Ensure JDK 24 is installed
2. Run: `./mvnw spring-boot:run`
3. Server starts on port 8080
4. H2 Console: http://localhost:8080/h2-console
5. JDBC URL: jdbc:h2:mem:werwoelflen

## Notes
- All timestamps are in UTC (Instant)
- State flags are stored as JSON strings for flexibility
- Frontend should poll /api/games/{gameId}/state for updates
- Authentication uses Bearer token in Authorization header

