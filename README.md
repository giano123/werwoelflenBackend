# Werwölfeln (Werewolf/Mafia) - Backend Implementation

## 🎮 Project Overview

Complete backend implementation for the digital version of the social deduction game "Werwölfeln" (Werewolf/Mafia). This is a fully functional REST API built with Spring Boot that handles all game logic, player management, and real-time state updates.

## ✨ Features

### ✅ Complete Game Implementation
- **5 Roles**: Werewolf, Villager, Seer, Witch, Hunter
- **6 Game Phases**: Night (Wolves/Seer/Witch), Day (Discussion/Voting), Result
- **2 Factions**: Village vs Werewolves
- **Full Game Logic**: Automated phase transitions, win condition detection, death resolution
- **Special Abilities**: Seer investigation, Witch potions, Hunter revenge

### ✅ Lobby System
- Create lobbies with unique join codes
- Join/leave functionality with host transfer
- Ready status management
- Automatic role distribution on game start
- Max player enforcement (4-12 players)

### ✅ Authentication & Security
- Token-based authentication
- Secure password hashing (SHA-256)
- Session management with 30-day expiry
- Protected endpoints with Bearer token validation

### ✅ Multi-Channel Chat
- **LOBBY**: Pre-game chat
- **DAY**: Public discussion during day phase
- **NIGHT_WOLVES**: Private werewolf team chat
- **SYSTEM**: Automated game event messages

### ✅ Player-Specific Views
- Each player sees only what their role permits
- Werewolves identify each other
- Seer sees investigation results
- Witch sees wolf victim
- All players see revealed roles (dead players)

## 🏗️ Architecture

### Technology Stack
- **Framework**: Spring Boot 3.5.6
- **Database**: H2 (In-Memory, relational)
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven
- **Java Version**: 17+

### Project Structure
```
src/main/java/com/ausganslage/ausgangslageBackend/
├── enums/              # Game enums (7 files)
│   ├── ActionType.java
│   ├── ChatChannel.java
│   ├── Faction.java
│   ├── GamePhase.java
│   ├── GameStatus.java
│   ├── LobbyStatus.java
│   └── RoleName.java
├── model/              # JPA entities (9 files)
│   ├── User.java
│   ├── Session.java
│   ├── Lobby.java
│   ├── LobbyMember.java
│   ├── RoleTemplate.java
│   ├── Game.java
│   ├── GamePlayer.java
│   ├── GameAction.java
│   └── ChatMessage.java
├── repository/         # Data access (9 files)
│   ├── UserRepository.java
│   ├── SessionRepository.java
│   ├── LobbyRepository.java
│   ├── LobbyMemberRepository.java
│   ├── RoleTemplateRepository.java
│   ├── GameRepository.java
│   ├── GamePlayerRepository.java
│   ├── GameActionRepository.java
│   └── ChatMessageRepository.java
├── dto/                # API contracts (14 files)
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   ├── UserDto.java
│   ├── CreateLobbyRequest.java
│   ├── LobbyStateDto.java
│   ├── LobbyMemberDto.java
│   ├── GameStateDto.java
│   ├── PlayerInfoDto.java
│   ├── VoteActionRequest.java
│   ├── PowerActionRequest.java
│   ├── ChatMessageRequest.java
│   ├── ChatMessageDto.java
│   ├── InspectionResultDto.java
│   └── WolfVictimDto.java
├── service/            # Business logic (3 files)
│   ├── AuthService.java
│   ├── LobbyService.java
│   └── GameService.java (Core game engine)
├── controller/         # REST endpoints (3 files)
│   ├── AuthController.java
│   ├── LobbyController.java
│   └── GameController.java
├── security/           # Authentication (2 files)
│   ├── AuthenticationFilter.java
│   └── SecurityConfig.java
├── config/             # Configuration (2 files)
│   ├── CorsConfig.java
│   └── DataLoader.java
├── util/               # Utilities (2 files)
│   ├── CodeGenerator.java
│   └── PasswordUtil.java
└── exception/          # Error handling (1 file)
    └── GlobalExceptionHandler.java
```

### Database Schema (9 Tables)
1. **users**: Player accounts
2. **sessions**: Authentication tokens
3. **lobbies**: Game lobbies
4. **lobby_members**: Players in lobbies
5. **role_templates**: Role definitions
6. **games**: Active games
7. **game_players**: Players in games with roles
8. **game_actions**: All player actions/votes
9. **chat_messages**: Chat history

## 🚀 Getting Started

### Run the Application
```bash
# Using Maven Wrapper (recommended)
.\mvnw.cmd spring-boot:run

# Or using IntelliJ IDEA
Right-click AusgangslageBackendApplication.java → Run
```

Server starts on: **http://localhost:8080**

### Access H2 Console
URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:werwoelflen`
- Username: `sa`
- Password: (empty)

## 📡 API Endpoints

### Authentication
- `POST /api/auth/register` - Create new user account
- `POST /api/auth/login` - Login and get token
- `GET /api/auth/me` - Get current user info

### Lobby Management
- `POST /api/lobbies` - Create new lobby
- `GET /api/lobbies/{code}/state` - Get lobby state
- `POST /api/lobbies/{code}/join` - Join lobby
- `POST /api/lobbies/{code}/leave` - Leave lobby
- `POST /api/lobbies/{code}/ready` - Set ready status
- `POST /api/lobbies/{code}/start` - Start game (host only)

### Game Actions
- `GET /api/games/{gameId}/state` - Get player-specific game state
- `GET /api/games/lobby/{lobbyCode}` - Get game by lobby code
- `POST /api/games/{gameId}/actions/vote` - Submit vote (lynch/wolf kill)
- `POST /api/games/{gameId}/actions/power` - Use special ability
- `POST /api/games/{gameId}/actions/skip` - Skip turn (Seer/Witch)
- `POST /api/games/{gameId}/transition-to-voting` - Start voting phase

### Special Queries
- `GET /api/games/{gameId}/wolf-victim` - See wolf kill target (Witch only)
- `GET /api/games/{gameId}/inspection-result` - See Seer's last inspection

### Chat
- `GET /api/games/{gameId}/chat?since={timestamp}` - Get chat messages
- `POST /api/games/{gameId}/chat` - Send chat message

## 🎯 Game Logic Highlights

### Role Distribution (Automatic)
```
Werewolves: max(1, playerCount / 4)
Seer: 1
Witch: 1
Hunter: 1
Villagers: remaining
```

### Phase Progression
```
NIGHT_WOLVES → NIGHT_SEER → NIGHT_WITCH → 
  [Night Resolution] → 
DAY_DISCUSSION → DAY_VOTING → 
  [Day Resolution] → 
  [Check Win] → 
NIGHT_WOLVES (next day)
```

### Win Conditions
- **Village**: All werewolves eliminated
- **Werewolves**: Equal or outnumber villagers

### Special Mechanics
- **Werewolf Vision**: Werewolves see each other
- **Witch Potions**: One heal, one poison (single-use)
- **Hunter Revenge**: Shoots when killed
- **Death Revelation**: Dead players' roles are revealed
- **Majority Voting**: Both wolf kills and lynches use majority

## 📚 Documentation Files

- **API_DOCUMENTATION.md**: Complete API reference
- **IMPLEMENTATION_SUMMARY.md**: Architecture and file listing
- **GAME_FLOW.md**: Detailed game logic and phase diagrams
- **QUICK_START.md**: Setup and testing guide
- **Werwoelfeln_API.postman_collection.json**: Postman collection for testing

## 🧪 Testing

### Import Postman Collection
1. Open Postman
2. Import `Werwoelfeln_API.postman_collection.json`
3. Set environment variables: `token`, `lobbyCode`, `gameId`
4. Test all endpoints

### Manual Testing Flow
1. Register 4+ users
2. User 1 creates lobby
3. Users 2-4 join with lobby code
4. All users set ready
5. User 1 starts game
6. Poll `/api/games/{gameId}/state` for updates
7. Submit actions based on role and phase

## 🔧 Configuration

### application.properties
```properties
spring.application.name=werwoelflenBackend
spring.datasource.url=jdbc:h2:mem:werwoelflen
spring.jpa.hibernate.ddl-auto=create-drop
server.port=8080
```

### CORS Configuration
Frontend origin: `http://localhost:5173` (Vite default)

## 📊 Key Implementation Details

### State Management
- **Game State**: Stored in `Game` entity (phase, day, status)
- **Player State**: Stored in `GamePlayer` (alive, role, flags)
- **Action History**: All actions recorded in `GameAction`
- **Flexible Flags**: JSON strings for role-specific state

### Transaction Safety
- All write operations use `@Transactional`
- Read operations marked `@Transactional(readOnly = true)`
- Proper isolation for concurrent actions

### Validation
- Phase-appropriate actions
- Role permissions
- Alive player checks
- One-time ability enforcement
- Target validity

### Error Handling
- Global exception handler
- Descriptive error messages
- Proper HTTP status codes
- Client-friendly error responses

## 🎲 Role Abilities

### Werewolf
- **Phase**: NIGHT_WOLVES
- **Action**: Vote to kill a villager
- **Mechanic**: Majority vote determines victim

### Seer
- **Phase**: NIGHT_SEER
- **Action**: Investigate one player's role
- **Result**: Privately learns target's role

### Witch
- **Phase**: NIGHT_WITCH
- **Actions**: 
  - Heal potion (save wolf victim, one-time)
  - Poison potion (kill any player, one-time)
- **Info**: Sees who werewolves targeted

### Hunter
- **Trigger**: On death (any cause)
- **Action**: Shoots one player as revenge
- **Timing**: Can act immediately after death

### Villager
- **Phase**: DAY_VOTING
- **Action**: Vote to lynch suspected werewolves
- **Power**: None (basic role)

## 🔐 Security

### Authentication Flow
1. User registers → password hashed → user created
2. User logs in → credentials validated → token generated
3. Token stored in session with 30-day expiry
4. All protected endpoints require: `Authorization: Bearer {token}`
5. Filter validates token and injects User into request

### Password Security
- SHA-256 hashing (PasswordUtil)
- Optional BCrypt support (Spring Security Crypto)
- Secure token generation (32-byte random)

## 🌐 Frontend Integration

### Polling Strategy
```javascript
// Poll game state every 2 seconds
const pollGameState = async () => {
  const response = await fetch(`/api/games/${gameId}/state`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const state = await response.json();
  
  // Update UI based on:
  // - state.currentPhase
  // - state.ownRole
  // - state.availableActions
  // - state.players (alive/dead status)
};

setInterval(pollGameState, 2000);
```

### Chat Polling
```javascript
let lastChatTimestamp = 0;

const pollChat = async () => {
  const response = await fetch(
    `/api/games/${gameId}/chat?since=${lastChatTimestamp}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  );
  const messages = await response.json();
  
  if (messages.length > 0) {
    messages.forEach(addChatMessage);
    lastChatTimestamp = messages[messages.length - 1].createdAt;
  }
};

setInterval(pollChat, 1000);
```

## 🐛 Troubleshooting

### Common Issues

**"Unauthorized" Error**
- Ensure Authorization header is included
- Check token hasn't expired
- Verify token was saved from login response

**"Lobby not found"**
- Verify lobby code is correct (case-sensitive)
- Check lobby hasn't been closed

**"Cannot vote during this phase"**
- Check `currentPhase` in game state
- Ensure your role can act during this phase
- Verify you're alive

**"Heal potion already used"**
- Witch potions are one-time use
- Check `ownStateFlags` in game state

## 📈 Performance Considerations

- In-memory H2 database (fast, resets on restart)
- Indexed lookups on codes and tokens
- Efficient query methods in repositories
- Lazy loading where appropriate

## 🔄 Future Enhancements (Optional)

- [ ] WebSocket support for real-time updates (no polling)
- [ ] Persistent database (PostgreSQL/MySQL)
- [ ] Timer-based phase transitions
- [ ] Spectator mode
- [ ] Game history and statistics
- [ ] Additional roles (Cupid, Drunk, etc.)
- [ ] Custom role configurations per lobby
- [ ] Replay system

## 📝 Development Notes

### Code Quality
- Clean, readable code with minimal comments
- Lombok for boilerplate reduction
- Proper separation of concerns (MVC pattern)
- Comprehensive validation
- Transaction management

### Database Design
- Normalized schema (3NF)
- Flexible JSON fields for extensibility
- Proper foreign key relationships
- Audit timestamps on all entities

### API Design
- RESTful conventions
- Clear request/response DTOs
- Consistent error handling
- Player-specific data filtering

## 🎓 Educational Value

This implementation demonstrates:
- **Spring Boot** best practices
- **JPA/Hibernate** entity relationships
- **REST API** design
- **State machine** implementation (game phases)
- **Complex business logic** (game rules)
- **Authentication** patterns
- **Transaction management**
- **Error handling** strategies

## 📖 Documentation

| File | Purpose |
|------|---------|
| API_DOCUMENTATION.md | Complete API reference with examples |
| IMPLEMENTATION_SUMMARY.md | Architecture overview and file listing |
| GAME_FLOW.md | Detailed game logic and phase transitions |
| QUICK_START.md | Setup and testing instructions |
| README.md | This file - project overview |

## 🤝 Contributing

To extend the implementation:
1. Add new roles in `RoleTemplate` via DataLoader
2. Add new action types in `ActionType` enum
3. Implement validation in `GameService.validatePowerAction()`
4. Add phase logic in `GameService.checkAndAdvancePhase()`
5. Update `GameService.calculateAvailableActions()`

## 📄 License

Educational project for module 322

## 👥 Credits

Implemented as a complete, production-ready backend for the Werwölfeln digital game project.

---

**Status**: ✅ COMPLETE - All features implemented and functional

**Last Updated**: November 25, 2025

