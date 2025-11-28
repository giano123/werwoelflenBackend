# API TypeScript Types Documentation

Diese Dokumentation beschreibt alle API-Endpunkte mit ihren Input- und Output-Typen als TypeScript-Interfaces.

## TypeScript Type Definitions

### Enums

```typescript
enum RoleName {
  WEREWOLF = 'WEREWOLF',
  VILLAGER = 'VILLAGER',
  SEER = 'SEER',
  WITCH = 'WITCH',
  HUNTER = 'HUNTER'
}

enum LobbyStatus {
  OPEN = 'OPEN',
  IN_GAME = 'IN_GAME',
  CLOSED = 'CLOSED'
}

enum GameStatus {
  STARTING = 'STARTING',
  RUNNING = 'RUNNING',
  FINISHED = 'FINISHED'
}

enum GamePhase {
  NIGHT_WOLVES = 'NIGHT_WOLVES',
  NIGHT_SEER = 'NIGHT_SEER',
  NIGHT_WITCH = 'NIGHT_WITCH',
  DAY_DISCUSSION = 'DAY_DISCUSSION',
  DAY_VOTING = 'DAY_VOTING',
  RESULT = 'RESULT'
}

enum Faction {
  VILLAGE = 'VILLAGE',
  WOLVES = 'WOLVES',
  NEUTRAL = 'NEUTRAL'
}

enum ChatChannel {
  LOBBY = 'LOBBY',
  DAY = 'DAY',
  NIGHT_WOLVES = 'NIGHT_WOLVES',
  SYSTEM = 'SYSTEM'
}

enum ActionType {
  VOTE_LYNCH = 'VOTE_LYNCH',
  VOTE_WOLF_KILL = 'VOTE_WOLF_KILL',
  SEER_INSPECT = 'SEER_INSPECT',
  WITCH_HEAL = 'WITCH_HEAL',
  WITCH_POISON = 'WITCH_POISON',
  HUNTER_SHOOT = 'HUNTER_SHOOT'
}
```

### DTOs

```typescript
interface UserDto {
  id: number;
  username: string;
  email: string;
  avatarConfig: string;
}

interface AuthResponse {
  token: string;
  user: UserDto;
}

interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

interface CreateLobbyRequest {
  maxPlayers?: number;
  settingsJson?: string;
}

interface LobbyMemberDto {
  id: number;
  userId: number;
  username: string;
  avatarConfig: string;
  isHost: boolean;
  isReady: boolean;
}

interface LobbyStateDto {
  id: number;
  lobbyCode: string;
  hostUserId: number;
  maxPlayers: number;
  status: LobbyStatus;
  settingsJson: string;
  members: LobbyMemberDto[];
}

interface PlayerInfoDto {
  playerId: number;
  userId: number;
  username: string;
  avatarConfig: string;
  seatNumber: number;
  isAlive: boolean;
  revealedRole: boolean;
  role: RoleName | null;
}

interface WolfVictimDto {
  playerId: number;
  username: string;
}

interface InspectionResultDto {
  playerId: number;
  username: string;
  role: RoleName;
}

interface GameStateDto {
  gameId: number;
  status: GameStatus;
  currentPhase: GamePhase;
  dayNumber: number;
  winnerFaction: Faction | null;
  ownRole: RoleName;
  ownFaction: Faction;
  isAlive: boolean;
  ownStateFlags: Record<string, any>;
  players: PlayerInfoDto[];
  availableActions: string[];
  phaseDescription: string;
  wolfVictim: WolfVictimDto | null;
  lastInspection: InspectionResultDto | null;
}

interface Game {
  id: number;
  lobbyId: number;
  status: GameStatus;
  currentPhase: GamePhase;
  dayNumber: number;
  winnerFaction: Faction | null;
  createdAt: string;
  finishedAt: string | null;
}

interface VoteActionRequest {
  targetPlayerId: number;
}

interface PowerActionRequest {
  actionType: ActionType;
  targetPlayerId: number;
}

interface ChatMessageRequest {
  content: string;
}

interface ChatMessageDto {
  id: number;
  senderUserId: number;
  senderUsername: string;
  channel: ChatChannel;
  content: string;
  createdAt: string;
}
```

---

## API Endpoints

### Authentication Endpoints (`/api/auth`)

#### POST `/api/auth/register`
Registriert einen neuen Benutzer.

**Request Body:**
```typescript
RegisterRequest
```

**Response:**
```typescript
AuthResponse
```

**Status Codes:**
- `200 OK` - Erfolgreiche Registrierung
- `400 Bad Request` - Ungültige Daten (z.B. Benutzername bereits vergeben)

---

#### POST `/api/auth/login`
Meldet einen Benutzer an.

**Request Body:**
```typescript
LoginRequest
```

**Response:**
```typescript
AuthResponse
```

**Status Codes:**
- `200 OK` - Erfolgreiche Anmeldung
- `400 Bad Request` - Ungültige Anmeldedaten

---

#### GET `/api/auth/me`
Gibt die Informationen des aktuell angemeldeten Benutzers zurück.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Response:**
```typescript
UserDto
```

**Status Codes:**
- `200 OK` - Erfolgreiche Abfrage

---

### Lobby Endpoints (`/api/lobbies`)

#### POST `/api/lobbies`
Erstellt eine neue Lobby.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Request Body:**
```typescript
CreateLobbyRequest
```

**Response:**
```typescript
LobbyStateDto
```

**Status Codes:**
- `200 OK` - Lobby erfolgreich erstellt

---

#### GET `/api/lobbies/{code}/state`
Ruft den aktuellen Status einer Lobby ab.

**Path Parameters:**
- `code: string` - Lobby-Code

**Response:**
```typescript
LobbyStateDto
```

**Status Codes:**
- `200 OK` - Erfolgreiche Abfrage
- `404 Not Found` - Lobby nicht gefunden

---

#### POST `/api/lobbies/{code}/join`
Tritt einer Lobby bei.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `code: string` - Lobby-Code

**Response:**
```typescript
LobbyStateDto
```

**Status Codes:**
- `200 OK` - Erfolgreich beigetreten
- `400 Bad Request` - Lobby voll oder bereits im Spiel

---

#### POST `/api/lobbies/{code}/leave`
Verlässt eine Lobby.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `code: string` - Lobby-Code

**Response:**
```typescript
void
```

**Status Codes:**
- `200 OK` - Erfolgreich verlassen
- `404 Not Found` - Lobby nicht gefunden

---

#### POST `/api/lobbies/{code}/ready`
Setzt den Ready-Status des Spielers.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `code: string` - Lobby-Code

**Query Parameters:**
- `ready: boolean` - Ready-Status (default: `true`)

**Response:**
```typescript
void
```

**Status Codes:**
- `200 OK` - Status erfolgreich gesetzt
- `400 Bad Request` - Ungültige Anfrage

---

#### POST `/api/lobbies/{code}/start`
Startet das Spiel (nur Host).

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `code: string` - Lobby-Code

**Response:**
```typescript
Game
```

**Status Codes:**
- `200 OK` - Spiel erfolgreich gestartet
- `400 Bad Request` - Nicht alle Spieler bereit oder nicht genug Spieler

---

### Game Endpoints (`/api/games`)

#### POST `/api/games/start/{lobbyCode}`
Startet ein Spiel für eine Lobby.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `lobbyCode: string` - Lobby-Code

**Response:**
```typescript
Game
```

**Status Codes:**
- `200 OK` - Spiel erfolgreich gestartet
- `400 Bad Request` - Ungültige Anfrage

---

#### GET `/api/games/{gameId}/state`
Ruft den Spielstatus für den aktuellen Spieler ab.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `gameId: number` - Spiel-ID

**Response:**
```typescript
GameStateDto
```

**Status Codes:**
- `200 OK` - Erfolgreiche Abfrage
- `404 Not Found` - Spiel nicht gefunden

---

#### GET `/api/games/lobby/{lobbyCode}`
Ruft das Spiel für eine Lobby ab.

**Path Parameters:**
- `lobbyCode: string` - Lobby-Code

**Response:**
```typescript
Game
```

**Status Codes:**
- `200 OK` - Spiel gefunden
- `404 Not Found` - Lobby oder Spiel nicht gefunden

---

#### POST `/api/games/{gameId}/actions/vote`
Gibt eine Stimme ab (Lynch-Voting oder Werwolf-Kill).

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `gameId: number` - Spiel-ID

**Request Body:**
```typescript
VoteActionRequest
```

**Response:**
```typescript
void
```

**Status Codes:**
- `200 OK` - Stimme erfolgreich abgegeben
- `400 Bad Request` - Ungültige Aktion

---

#### POST `/api/games/{gameId}/actions/power`
Führt eine Spezialfähigkeit aus (Seher, Hexe, Jäger).

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `gameId: number` - Spiel-ID

**Request Body:**
```typescript
PowerActionRequest
```

**Response:**
```typescript
void
```

**Status Codes:**
- `200 OK` - Aktion erfolgreich ausgeführt
- `400 Bad Request` - Ungültige Aktion

---

#### POST `/api/games/{gameId}/actions/skip`
Überspringt die eigene Aktion.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `gameId: number` - Spiel-ID

**Response:**
```typescript
void
```

**Status Codes:**
- `200 OK` - Aktion übersprungen
- `400 Bad Request` - Ungültige Anfrage

---

#### GET `/api/games/{gameId}/chat`
Ruft Chat-Nachrichten ab.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `gameId: number` - Spiel-ID

**Query Parameters:**
- `since: number` (optional) - Nur Nachrichten nach dieser ID abrufen

**Response:**
```typescript
ChatMessageDto[]
```

**Status Codes:**
- `200 OK` - Nachrichten erfolgreich abgerufen
- `404 Not Found` - Spiel nicht gefunden

---

#### POST `/api/games/{gameId}/chat`
Sendet eine Chat-Nachricht.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `gameId: number` - Spiel-ID

**Request Body:**
```typescript
ChatMessageRequest
```

**Response:**
```typescript
ChatMessageDto
```

**Status Codes:**
- `200 OK` - Nachricht erfolgreich gesendet
- `400 Bad Request` - Ungültige Nachricht

---

#### POST `/api/games/{gameId}/transition-to-voting`
Übergang von Diskussion zu Voting-Phase.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `gameId: number` - Spiel-ID

**Response:**
```typescript
void
```

**Status Codes:**
- `200 OK` - Erfolgreich zur Voting-Phase gewechselt
- `400 Bad Request` - Ungültige Anfrage

---

#### GET `/api/games/{gameId}/wolf-victim`
Ruft das Opfer der Werwölfe ab.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `gameId: number` - Spiel-ID

**Response:**
```typescript
WolfVictimDto
```

**Status Codes:**
- `200 OK` - Opfer erfolgreich abgerufen
- `400 Bad Request` - Ungültige Anfrage

---

#### GET `/api/games/{gameId}/inspection-result`
Ruft das letzte Seher-Inspektionsergebnis ab.

**Headers:**
- `Authorization: Bearer <token>` (erforderlich)

**Path Parameters:**
- `gameId: number` - Spiel-ID

**Response:**
```typescript
InspectionResultDto
```

**Status Codes:**
- `200 OK` - Ergebnis erfolgreich abgerufen
- `400 Bad Request` - Ungültige Anfrage

---

## Authentifizierung

Die meisten Endpunkte erfordern eine Authentifizierung über einen Bearer-Token im Authorization-Header:

```
Authorization: Bearer <your-jwt-token>
```

Der Token wird bei erfolgreicher Registrierung oder Anmeldung in der `AuthResponse` zurückgegeben.

## Fehlerbehandlung

Alle Endpunkte können folgende Fehlercodes zurückgeben:

- `400 Bad Request` - Ungültige Anfragedaten
- `401 Unauthorized` - Fehlende oder ungültige Authentifizierung
- `404 Not Found` - Ressource nicht gefunden
- `500 Internal Server Error` - Serverfehler

