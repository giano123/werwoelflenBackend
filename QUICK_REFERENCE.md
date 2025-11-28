# 🎮 WERWÖLFELN BACKEND - QUICK REFERENCE CARD

## 🚀 START APPLICATION
```bash
# In IntelliJ: Right-click AusgangslageBackendApplication.java → Run
# Or via Maven: .\mvnw.cmd spring-boot:run
```
**Server**: http://localhost:8080
**H2 Console**: http://localhost:8080/h2-console

---

## 🔑 AUTHENTICATION

### Register
```bash
POST /api/auth/register
{"username":"alice","email":"alice@test.com","password":"pass123"}
→ Returns: {"token":"...", "user":{...}}
```

### Login
```bash
POST /api/auth/login
{"usernameOrEmail":"alice","password":"pass123"}
→ Returns: {"token":"...", "user":{...}}
```

### All Protected Routes
```bash
Header: Authorization: Bearer {token}
```

---

## 🏠 LOBBY FLOW

### 1. Create Lobby
```bash
POST /api/lobbies + token
→ Returns: {lobbyCode:"ABC123", ...}
```

### 2. Join Lobby
```bash
POST /api/lobbies/ABC123/join + token
```

### 3. Set Ready
```bash
POST /api/lobbies/ABC123/ready?ready=true + token
```

### 4. Start Game (Host Only)
```bash
POST /api/lobbies/ABC123/start + token
→ Returns: {id:1, status:"RUNNING", ...}
```

---

## 🎲 GAME FLOW

### Get Game State (Poll every 2s)
```bash
GET /api/games/1/state + token
→ Returns player-specific view
```

**Response includes:**
- `currentPhase`: What phase is active
- `ownRole`: Your role (WEREWOLF, SEER, etc.)
- `availableActions[]`: What you can do now
- `players[]`: All players (alive/dead)
- `wolfVictim`: Who wolves targeted (Witch only)
- `lastInspection`: Seer's result

---

## 🐺 WEREWOLF ACTIONS

### Night Phase - Vote to Kill
```bash
POST /api/games/1/actions/vote + token
{"targetPlayerId":5}

Phase: NIGHT_WOLVES
Role: WEREWOLF
Effect: Vote for victim (majority wins)
```

---

## 🔮 SEER ACTIONS

### Night Phase - Investigate
```bash
POST /api/games/1/actions/power + token
{"actionType":"SEER_INSPECT","targetPlayerId":3}

Phase: NIGHT_SEER
Role: SEER
Effect: Learn target's role
```

### Get Investigation Result
```bash
GET /api/games/1/inspection-result + token
→ Returns: {"playerId":3,"username":"Bob","role":"WEREWOLF"}
```

---

## 🧪 WITCH ACTIONS

### See Wolf Victim
```bash
GET /api/games/1/wolf-victim + token
→ Returns: {"playerId":4,"username":"Charlie"}

Phase: NIGHT_WITCH
```

### Heal Wolf Victim
```bash
POST /api/games/1/actions/power + token
{"actionType":"WITCH_HEAL","targetPlayerId":4}

One-time use only!
```

### Poison Player
```bash
POST /api/games/1/actions/power + token
{"actionType":"WITCH_POISON","targetPlayerId":2}

One-time use only!
```

---

## 🏹 HUNTER ACTIONS

### Revenge Shot (After Death)
```bash
POST /api/games/1/actions/power + token
{"actionType":"HUNTER_SHOOT","targetPlayerId":1}

Available: When hunter dies
Effect: Target dies immediately
```

---

## 🗳️ DAY PHASE ACTIONS

### Vote to Lynch
```bash
POST /api/games/1/actions/vote + token
{"targetPlayerId":2}

Phase: DAY_VOTING
Effect: Vote to eliminate (majority wins)
```

### Transition to Voting
```bash
POST /api/games/1/transition-to-voting + token

Phase: DAY_DISCUSSION → DAY_VOTING
```

---

## 💬 CHAT

### Send Message
```bash
POST /api/games/1/chat + token
{"content":"I think Alice is suspicious!"}

Channels:
- DAY: All alive during day
- NIGHT_WOLVES: Werewolf team
- SYSTEM: Automated messages
```

### Get Messages
```bash
GET /api/games/1/chat?since=0 + token
→ Returns: [{id,senderUsername,channel,content,createdAt}]
```

---

## 🎯 GAME PHASES

| Phase | Active Role | Actions Available |
|-------|-------------|-------------------|
| NIGHT_WOLVES | Werewolves | Vote to kill |
| NIGHT_SEER | Seer | Investigate player |
| NIGHT_WITCH | Witch | Heal/Poison |
| DAY_DISCUSSION | All Alive | Chat |
| DAY_VOTING | All Alive | Vote to lynch |
| RESULT | None | Game over |

---

## 🏆 WIN CONDITIONS

**Village Wins**: All werewolves eliminated
**Werewolves Win**: Werewolves ≥ Villagers

Auto-detected after each resolution

---

## 🎭 ROLES (Auto-Distributed)

| Players | Werewolves | Seer | Witch | Hunter | Villagers |
|---------|------------|------|-------|--------|-----------|
| 4 | 1 | 1 | 1 | 1 | 0 |
| 5 | 1 | 1 | 1 | 1 | 1 |
| 6 | 1 | 1 | 1 | 1 | 2 |
| 8 | 2 | 1 | 1 | 1 | 3 |
| 12 | 3 | 1 | 1 | 1 | 6 |

Formula: Werewolves = max(1, playerCount / 4)

---

## 🔄 TYPICAL GAME SEQUENCE

```
1. NIGHT_WOLVES: Wolves vote → Player 5 targeted
2. NIGHT_SEER: Seer inspects → Learns Player 2 = WEREWOLF
3. NIGHT_WITCH: Witch heals Player 5 → Player 5 survives!
   [Night Resolution]
4. DAY_DISCUSSION: Players chat, Seer hints
5. DAY_VOTING: All vote → Player 2 lynched (was Werewolf)
   [Day Resolution, check win]
6. NIGHT_WOLVES (Day 2): Continue...
```

---

## ⚠️ COMMON ERRORS

| Error | Cause | Solution |
|-------|-------|----------|
| 401 Unauthorized | Missing/invalid token | Include Authorization header |
| 400 Bad Request | Invalid data | Check request body format |
| 409 Conflict | Wrong game state | Check currentPhase |
| "Dead players cannot vote" | Player is dead | Only alive can act |
| "Heal potion already used" | Potion used | One-time abilities |
| "Only host can start" | Not the host | Host must start game |

---

## 📊 DATABASE QUERIES

### See All Users
```sql
SELECT * FROM USERS;
```

### See Role Distribution
```sql
SELECT gp.SEAT_NUMBER, u.USERNAME, rt.NAME as ROLE, gp.IS_ALIVE
FROM GAME_PLAYERS gp
JOIN USERS u ON gp.USER_ID = u.ID
JOIN ROLE_TEMPLATES rt ON gp.ROLE_ID = rt.ID
WHERE gp.GAME_ID = 1
ORDER BY gp.SEAT_NUMBER;
```

### See Recent Actions
```sql
SELECT ga.DAY_NUMBER, ga.PHASE, ga.ACTION_TYPE, 
       u1.USERNAME as ACTOR, u2.USERNAME as TARGET
FROM GAME_ACTIONS ga
LEFT JOIN GAME_PLAYERS gp1 ON ga.ACTOR_PLAYER_ID = gp1.ID
LEFT JOIN GAME_PLAYERS gp2 ON ga.TARGET_PLAYER_ID = gp2.ID
LEFT JOIN USERS u1 ON gp1.USER_ID = u1.ID
LEFT JOIN USERS u2 ON gp2.USER_ID = u2.ID
WHERE ga.GAME_ID = 1
ORDER BY ga.CREATED_AT DESC;
```

---

## 🧪 QUICK TEST

```powershell
# Run automated test script
.\test-api.ps1

# Expected: Creates 4 users, lobby, starts game
# Shows each player's role and available actions
```

---

## 📞 HELP

| Question | Answer |
|----------|--------|
| How do I start? | Run the app, then `.\test-api.ps1` |
| Which endpoints? | See API_DOCUMENTATION.md |
| How does game work? | See GAME_FLOW.md |
| Role mechanics? | See IMPLEMENTATION_VERIFICATION.md |
| Full setup? | See QUICK_START.md |

---

**🎯 EVERYTHING YOU NEED TO KNOW IN ONE FILE**

Ready to play! 🐺🎮

