# Quick Start Guide - Werwölfeln Backend

## Prerequisites
- Java 17 or higher (JDK)
- Maven 3.6+ (included via Maven Wrapper)

## Running the Application

### Option 1: IntelliJ IDEA
1. Open the project in IntelliJ IDEA
2. Wait for Maven to download dependencies
3. Right-click `AusgangslageBackendApplication.java`
4. Select "Run 'AusgangslageBackendApplication'"
5. Server starts on http://localhost:8080

### Option 2: Command Line
```bash
# Navigate to project directory
cd C:\Users\giano\IdeaProjects\werwoelflenBackend

# Run with Maven Wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Or with Maven Wrapper (Unix/Mac)
./mvnw spring-boot:run
```

### Option 3: Build JAR and Run
```bash
.\mvnw.cmd clean package
java -jar target/ausgangslageBackend-0.0.1-SNAPSHOT.jar
```

## Verify Installation

### Check Server is Running
```bash
curl http://localhost:8080/api/auth/login
```
Expected: 400 Bad Request (server is running, just missing credentials)

### Access H2 Console
Open browser: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:werwoelflen`
- Username: `sa`
- Password: (leave empty)

## Testing the API

### Step 1: Create Test Users
```bash
# Register Player 1
curl -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"alice\",\"email\":\"alice@test.com\",\"password\":\"pass123\"}"

# Save the token from response

# Register Player 2
curl -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"bob\",\"email\":\"bob@test.com\",\"password\":\"pass123\"}"

# Register Players 3 & 4
curl -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"charlie\",\"email\":\"charlie@test.com\",\"password\":\"pass123\"}"

curl -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"diana\",\"email\":\"diana@test.com\",\"password\":\"pass123\"}"
```

### Step 2: Create and Join Lobby
```bash
# Alice creates lobby
curl -X POST http://localhost:8080/api/lobbies ^
  -H "Authorization: Bearer {alice-token}" ^
  -H "Content-Type: application/json" ^
  -d "{\"maxPlayers\":8}"

# Save the lobbyCode from response (e.g., "ABC123")

# Other players join
curl -X POST http://localhost:8080/api/lobbies/ABC123/join ^
  -H "Authorization: Bearer {bob-token}"

curl -X POST http://localhost:8080/api/lobbies/ABC123/join ^
  -H "Authorization: Bearer {charlie-token}"

curl -X POST http://localhost:8080/api/lobbies/ABC123/join ^
  -H "Authorization: Bearer {diana-token}"
```

### Step 3: Set Ready and Start
```bash
# All players set ready
curl -X POST "http://localhost:8080/api/lobbies/ABC123/ready?ready=true" ^
  -H "Authorization: Bearer {alice-token}"

curl -X POST "http://localhost:8080/api/lobbies/ABC123/ready?ready=true" ^
  -H "Authorization: Bearer {bob-token}"

curl -X POST "http://localhost:8080/api/lobbies/ABC123/ready?ready=true" ^
  -H "Authorization: Bearer {charlie-token}"

curl -X POST "http://localhost:8080/api/lobbies/ABC123/ready?ready=true" ^
  -H "Authorization: Bearer {diana-token}"

# Alice (host) starts the game
curl -X POST http://localhost:8080/api/lobbies/ABC123/start ^
  -H "Authorization: Bearer {alice-token}"

# Save the gameId from response
```

### Step 4: Check Game State
```bash
# Each player checks their state
curl http://localhost:8080/api/games/1/state ^
  -H "Authorization: Bearer {alice-token}"

# Response shows ownRole, availableActions, etc.
```

### Step 5: Play the Game
```bash
# If you're a Werewolf during NIGHT_WOLVES
curl -X POST http://localhost:8080/api/games/1/actions/vote ^
  -H "Authorization: Bearer {werewolf-token}" ^
  -H "Content-Type: application/json" ^
  -d "{\"targetPlayerId\":3}"

# If you're the Seer during NIGHT_SEER
curl -X POST http://localhost:8080/api/games/1/actions/power ^
  -H "Authorization: Bearer {seer-token}" ^
  -H "Content-Type: application/json" ^
  -d "{\"actionType\":\"SEER_INSPECT\",\"targetPlayerId\":2}"

# Check inspection result
curl http://localhost:8080/api/games/1/inspection-result ^
  -H "Authorization: Bearer {seer-token}"

# If you're the Witch during NIGHT_WITCH
curl http://localhost:8080/api/games/1/wolf-victim ^
  -H "Authorization: Bearer {witch-token}"

curl -X POST http://localhost:8080/api/games/1/actions/power ^
  -H "Authorization: Bearer {witch-token}" ^
  -H "Content-Type: application/json" ^
  -d "{\"actionType\":\"WITCH_HEAL\",\"targetPlayerId\":3}"

# During DAY_VOTING
curl -X POST http://localhost:8080/api/games/1/actions/vote ^
  -H "Authorization: Bearer {player-token}" ^
  -H "Content-Type: application/json" ^
  -d "{\"targetPlayerId\":1}"
```

## Import Postman Collection

Import the file `Werwoelfeln_API.postman_collection.json` into Postman:
1. Open Postman
2. Click Import
3. Select the JSON file
4. Set variables: `token`, `lobbyCode`, `gameId`

## Database Inspection

Query the database via H2 Console:
```sql
-- See all users
SELECT * FROM USERS;

-- See active lobbies
SELECT * FROM LOBBIES;

-- See game state
SELECT * FROM GAMES;

-- See players in game
SELECT * FROM GAME_PLAYERS;

-- See all actions
SELECT * FROM GAME_ACTIONS ORDER BY CREATED_AT DESC;

-- See role distribution
SELECT gp.*, rt.NAME as ROLE_NAME 
FROM GAME_PLAYERS gp 
JOIN ROLE_TEMPLATES rt ON gp.ROLE_ID = rt.ID
WHERE gp.GAME_ID = 1;
```

## Common Issues & Solutions

### Issue: "No compiler provided"
**Solution**: Ensure JAVA_HOME points to JDK (not JRE)
```bash
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17

# Verify
java -version
javac -version
```

### Issue: Port 8080 already in use
**Solution**: Change port in application.properties
```properties
server.port=8081
```

### Issue: 401 Unauthorized
**Solution**: Include Authorization header with valid token
```bash
-H "Authorization: Bearer your-token-here"
```

### Issue: Lobby start fails
**Solution**: Ensure:
- At least 4 players in lobby
- All players are ready
- Caller is the host
- Lobby status is OPEN

## Monitoring Game Progress

Poll the game state endpoint every 1-2 seconds:
```javascript
// Frontend polling example
setInterval(async () => {
  const response = await fetch(`/api/games/${gameId}/state`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const state = await response.json();
  updateUI(state);
}, 2000);
```

## Development Tips

### Enable Debug Logging
Add to application.properties:
```properties
logging.level.com.ausganslage.ausgangslageBackend=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

### Reset Database
Database resets on every restart (h2 in-memory)
To persist data, change to file-based:
```properties
spring.datasource.url=jdbc:h2:file:./data/werwoelflen
```

### Test Data
On startup, 5 role templates are automatically created:
- WEREWOLF
- VILLAGER
- SEER
- WITCH
- HUNTER

Check with:
```bash
curl http://localhost:8080/h2-console
# Query: SELECT * FROM ROLE_TEMPLATES;
```

## Next Steps

1. ✅ Backend is complete and functional
2. 🔄 Build the React frontend
3. 🔄 Implement polling mechanism
4. 🔄 Create UI for each role
5. 🔄 Add timer for phase transitions
6. 🔄 Enhance with WebSockets (optional upgrade)

## Support

For issues or questions about the implementation:
- Check API_DOCUMENTATION.md for endpoint details
- Check GAME_FLOW.md for game logic
- Check IMPLEMENTATION_SUMMARY.md for architecture overview

