# Werwölfeln Game Flow - Detailed Logic

## Phase Transition Diagram

```
LOBBY (OPEN)
    ↓ [Host clicks Start, all players ready]
GAME CREATED → NIGHT_WOLVES (Day 1)
    ↓ [All werewolves vote]
NIGHT_SEER
    ↓ [Seer inspects OR skip OR no seer alive]
NIGHT_WITCH
    ↓ [Witch acts OR skip OR no witch alive]
NIGHT RESOLUTION → DAY_DISCUSSION
    ↓ [Manual transition OR timer]
DAY_VOTING
    ↓ [All alive players vote]
DAY RESOLUTION → Check Win Condition
    ↓ [If no winner]
NIGHT_WOLVES (Day 2)
    ... [repeat]
    ↓ [Win condition met]
RESULT → GAME FINISHED
```

## Detailed Phase Logic

### NIGHT_WOLVES
**Active Players**: All alive Werewolves
**Actions**: 
- Each werewolf submits VOTE_WOLF_KILL for target
- Can revote (replaces previous vote)
- Cannot kill other werewolves

**Transition**: When all werewolves have voted
**Backend Logic**:
```java
hasAllWerewolvesVoted() 
  → Count alive werewolves
  → Count VOTE_WOLF_KILL actions for this phase
  → If equal, advance to NIGHT_SEER
```

### NIGHT_SEER
**Active Players**: Seer (if alive)
**Actions**:
- SEER_INSPECT on one target
- Can skip turn (POST /actions/skip)

**Transition**: When seer acts, skips, or is dead
**Result**: Seer privately learns target's role (stored in GameAction)

### NIGHT_WITCH
**Active Players**: Witch (if alive)
**Actions**:
- See wolf victim (GET /wolf-victim)
- WITCH_HEAL (one-time, only on wolf victim)
- WITCH_POISON (one-time, any player)
- Can skip turn

**Transition**: When witch acts/skips or is dead
**State Tracking**: 
```json
{
  "healPotion": true/false,
  "poisonPotion": true/false
}
```

### NIGHT RESOLUTION (Automatic)
**Actions Executed**:
1. Get majority wolf kill target
2. Check if witch healed that target
3. Apply wolf kill (if not healed)
4. Apply witch poison (if used)
5. Trigger hunter revenge if hunter dies
6. Create system messages
7. Check win condition

**Transition**: Automatically → DAY_DISCUSSION

### DAY_DISCUSSION
**Active Players**: All alive players
**Actions**:
- Send chat messages (CHANNEL: DAY)
- Discuss and strategize

**Transition**: Manual via POST /transition-to-voting
**Frontend**: Host or timer triggers transition

### DAY_VOTING
**Active Players**: All alive players
**Actions**:
- Each player submits VOTE_LYNCH
- Can revote

**Transition**: When all alive players have voted
**Resolution**:
1. Calculate majority target
2. Kill lynched player
3. Reveal their role
4. Trigger hunter revenge if applicable
5. Increment day number
6. Check win condition
7. → NIGHT_WOLVES (next day)

## Win Conditions

### Village Victory
```java
if (aliveWerewolves == 0) {
  status = FINISHED
  winnerFaction = VILLAGE
  phase = RESULT
}
```

### Werewolf Victory
```java
if (aliveWerewolves >= aliveVillagers) {
  status = FINISHED
  winnerFaction = WOLVES
  phase = RESULT
}
```

## Special Mechanics

### Hunter Revenge
**Trigger**: When Hunter dies (wolf kill OR lynch)
**Effect**:
1. Set `hunterShotAvailable = true` in stateFlags
2. Hunter can POST /actions/power with HUNTER_SHOOT
3. Target immediately dies
4. Can trigger another hunter if target is also hunter (chain reaction)

**Available During**: ANY phase after death (special case)

### Werewolf Team Vision
**Logic**: Werewolves see other werewolves' roles
```java
if (currentRole == WEREWOLF && targetRole == WEREWOLF) {
  dto.setRole(WEREWOLF)  // Visible to werewolf
}
```

### Death & Role Revelation
**On Death**:
```java
player.setIsAlive(false)
player.setRevealedRole(true)
```
**Effect**: All players see the revealed role in game state

## Action Validation Matrix

| Action | Phase | Role | Alive | Special |
|--------|-------|------|-------|---------|
| VOTE_WOLF_KILL | NIGHT_WOLVES | Werewolf | Yes | Cannot target werewolves |
| SEER_INSPECT | NIGHT_SEER | Seer | Yes | One per night |
| WITCH_HEAL | NIGHT_WITCH | Witch | Yes | One-time, only wolf victim |
| WITCH_POISON | NIGHT_WITCH | Witch | Yes | One-time, any player |
| VOTE_LYNCH | DAY_VOTING | Any | Yes | Cannot target dead |
| HUNTER_SHOOT | Any (after death) | Hunter | No | One-time revenge |

## State Management

### Game State
```java
Game {
  status: RUNNING
  currentPhase: NIGHT_WOLVES
  dayNumber: 1
  winnerFaction: null
}
```

### Player State
```java
GamePlayer {
  isAlive: true
  revealedRole: false
  stateFlagsJson: "{
    \"healPotion\": true,      // Witch only
    \"poisonPotion\": true,    // Witch only
    \"hunterShotAvailable\": true  // Hunter only
  }"
}
```

### Action Recording
Every action creates a GameAction record:
```java
GameAction {
  gameId: 1
  dayNumber: 1
  phase: NIGHT_WOLVES
  actorPlayerId: 3
  targetPlayerId: 7
  actionType: VOTE_WOLF_KILL
  payloadJson: "{}"
  createdAt: 2025-11-25T13:00:00Z
}
```

## Chat Channel Permissions

### SYSTEM
- **Visible to**: Everyone
- **Sender**: System (userId = 0)
- **Content**: Game events, deaths, phase changes

### DAY
- **Visible to**: All alive players during DAY_DISCUSSION & DAY_VOTING
- **Sender**: Any alive player
- **Content**: Public discussion

### NIGHT_WOLVES
- **Visible to**: All werewolves (alive or dead)
- **Sender**: Any werewolf
- **Content**: Werewolf team strategy

### LOBBY
- **Visible to**: All lobby members
- **Sender**: Any lobby member
- **Content**: Pre-game chat

## API Response Examples

### GET /api/games/{gameId}/state (Werewolf Player)
```json
{
  "gameId": 1,
  "status": "RUNNING",
  "currentPhase": "NIGHT_WOLVES",
  "dayNumber": 1,
  "winnerFaction": null,
  "ownRole": "WEREWOLF",
  "ownFaction": "WOLVES",
  "isAlive": true,
  "ownStateFlags": {},
  "players": [
    {
      "playerId": 1,
      "username": "Alice",
      "seatNumber": 1,
      "isAlive": true,
      "revealedRole": false,
      "role": "WEREWOLF"
    },
    {
      "playerId": 2,
      "username": "Bob",
      "seatNumber": 2,
      "isAlive": true,
      "revealedRole": false,
      "role": null
    }
  ],
  "availableActions": ["VOTE_WOLF_KILL"],
  "phaseDescription": "Night - Werewolves awaken",
  "wolfVictim": null,
  "lastInspection": null
}
```

### GET /api/games/{gameId}/state (Witch During NIGHT_WITCH)
```json
{
  "gameId": 1,
  "currentPhase": "NIGHT_WITCH",
  "ownRole": "WITCH",
  "ownStateFlags": {
    "healPotion": true,
    "poisonPotion": true
  },
  "availableActions": ["WITCH_HEAL", "WITCH_POISON"],
  "wolfVictim": {
    "playerId": 5,
    "username": "Charlie"
  }
}
```

### GET /api/games/{gameId}/state (Seer After Inspection)
```json
{
  "gameId": 1,
  "currentPhase": "DAY_DISCUSSION",
  "ownRole": "SEER",
  "lastInspection": {
    "playerId": 3,
    "username": "David",
    "role": "WEREWOLF"
  }
}
```

## Role Distribution Examples

### 4 Players
- 1 Werewolf
- 1 Seer
- 1 Witch
- 1 Hunter

### 8 Players
- 2 Werewolves (8/4 = 2)
- 1 Seer
- 1 Witch
- 1 Hunter
- 3 Villagers

### 12 Players
- 3 Werewolves (12/4 = 3)
- 1 Seer
- 1 Witch
- 1 Hunter
- 6 Villagers

## Error Handling

### Common Errors
```json
// 400 Bad Request
{"error": "Invalid credentials"}
{"error": "Lobby not found"}
{"error": "Dead players cannot vote"}

// 401 Unauthorized
{"error": "Unauthorized"}

// 409 Conflict
{"error": "Lobby is not open"}
{"error": "All players must be ready"}
{"error": "Heal potion already used"}
```

## Database Relationships

```
User (1) ←→ (N) Session
User (1) ←→ (N) LobbyMember ←→ (1) Lobby
User (1) ←→ (N) GamePlayer ←→ (1) Game
User (1) ←→ (N) ChatMessage

Lobby (1) ←→ (1) Game
Game (1) ←→ (N) GamePlayer
Game (1) ←→ (N) GameAction
Game (1) ←→ (N) ChatMessage

RoleTemplate (1) ←→ (N) GamePlayer
GamePlayer (1) ←→ (N) GameAction (as actor)
GamePlayer (1) ←→ (N) GameAction (as target)
```

## Complete Game Simulation

### Setup Phase
1. 4 players register
2. Player1 creates lobby → Gets code "ABC123"
3. Players 2-4 join lobby
4. All players set ready=true
5. Player1 starts game

### Night 1
1. **NIGHT_WOLVES**: 
   - Player1 (Werewolf) votes to kill Player3
   - Phase advances
2. **NIGHT_SEER**: 
   - Player2 (Seer) inspects Player1 → sees "WEREWOLF"
   - Phase advances
3. **NIGHT_WITCH**: 
   - Player4 (Witch) sees Player3 as wolf victim
   - Witch uses heal potion on Player3
   - Phase advances
4. **NIGHT RESOLUTION**: 
   - Player3 saved by witch
   - System message: "The Witch saved someone!"
   - Phase → DAY_DISCUSSION

### Day 1
1. **DAY_DISCUSSION**:
   - Players chat
   - Player2 hints about Player1
   - Manual transition to voting
2. **DAY_VOTING**:
   - All 4 players vote
   - Majority votes Player1
3. **DAY RESOLUTION**:
   - Player1 (Werewolf) lynched
   - Role revealed: WEREWOLF
   - No werewolves remain
   - **VILLAGE WINS!**

## Implementation Completeness

✅ **Authentication & Authorization**
- Token-based auth with BCrypt
- Session management
- User registration/login

✅ **Lobby System**
- Create with unique codes
- Join/leave mechanics
- Host transfer on leave
- Ready status
- Max player enforcement

✅ **Game Initialization**
- Balanced role distribution
- Random assignment
- State flag initialization

✅ **Phase Management**
- Sequential progression
- Automatic transitions
- Manual discussion→voting

✅ **All Role Abilities**
- Werewolf collective voting
- Seer investigation
- Witch heal/poison (one-time)
- Hunter revenge shot
- Villager voting

✅ **Victory Detection**
- Checked after every resolution
- Faction-based win conditions
- Game status updates

✅ **Player-Specific Views**
- Role visibility rules
- Available actions calculation
- Contextual information

✅ **Chat System**
- Multi-channel support
- Role-based permissions
- System messages

✅ **Error Handling**
- Comprehensive validation
- Descriptive error messages
- HTTP status codes

✅ **Database Design**
- Normalized schema
- Flexible JSON fields
- Proper indexes

