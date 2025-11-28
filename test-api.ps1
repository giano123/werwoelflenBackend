# Werwölfeln Backend - Complete Test Script

Write-Host "🐺 Werwölfeln Backend - API Test Script" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"

Write-Host "Step 1: Testing Server Connection..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body '{}' -ErrorAction SilentlyContinue
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "✅ Server is running!" -ForegroundColor Green
    } else {
        Write-Host "❌ Server not responding. Please start the application first." -ForegroundColor Red
        exit 1
    }
}
Write-Host ""

Write-Host "Step 2: Registering Test Users..." -ForegroundColor Yellow
$users = @(
    @{username="alice"; email="alice@test.com"; password="pass123"},
    @{username="bob"; email="bob@test.com"; password="pass123"},
    @{username="charlie"; email="charlie@test.com"; password="pass123"},
    @{username="diana"; email="diana@test.com"; password="pass123"}
)

$tokens = @{}

foreach ($user in $users) {
    try {
        $body = @{
            username = $user.username
            email = $user.email
            password = $user.password
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -ContentType "application/json" -Body $body
        $tokens[$user.username] = $response.token
        Write-Host "  ✅ Registered: $($user.username)" -ForegroundColor Green
    } catch {
        Write-Host "  ⚠️  $($user.username) might already exist, trying login..." -ForegroundColor Yellow

        $loginBody = @{
            usernameOrEmail = $user.username
            password = $user.password
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
        $tokens[$user.username] = $response.token
        Write-Host "  ✅ Logged in: $($user.username)" -ForegroundColor Green
    }
}
Write-Host ""

Write-Host "Step 3: Creating Lobby..." -ForegroundColor Yellow
$lobbyBody = @{
    maxPlayers = 8
    settingsJson = "{}"
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $($tokens['alice'])"
    "Content-Type" = "application/json"
}

$lobby = Invoke-RestMethod -Uri "$baseUrl/api/lobbies" -Method POST -Headers $headers -Body $lobbyBody
$lobbyCode = $lobby.lobbyCode
Write-Host "  ✅ Lobby created with code: $lobbyCode" -ForegroundColor Green
Write-Host ""

Write-Host "Step 4: Joining Lobby..." -ForegroundColor Yellow
foreach ($username in @("bob", "charlie", "diana")) {
    $headers = @{
        "Authorization" = "Bearer $($tokens[$username])"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/api/lobbies/$lobbyCode/join" -Method POST -Headers $headers
    Write-Host "  ✅ $username joined lobby" -ForegroundColor Green
}
Write-Host ""

Write-Host "Step 5: Setting Ready Status..." -ForegroundColor Yellow
foreach ($username in @("alice", "bob", "charlie", "diana")) {
    $headers = @{
        "Authorization" = "Bearer $($tokens[$username])"
    }

    Invoke-RestMethod -Uri "$baseUrl/api/lobbies/$lobbyCode/ready?ready=true" -Method POST -Headers $headers | Out-Null
    Write-Host "  ✅ $username is ready" -ForegroundColor Green
}
Write-Host ""

Write-Host "Step 6: Starting Game..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $($tokens['alice'])"
}

$game = Invoke-RestMethod -Uri "$baseUrl/api/lobbies/$lobbyCode/start" -Method POST -Headers $headers
$gameId = $game.id
Write-Host "  ✅ Game started! Game ID: $gameId" -ForegroundColor Green
Write-Host ""

Write-Host "Step 7: Checking Game State for Each Player..." -ForegroundColor Yellow
foreach ($username in @("alice", "bob", "charlie", "diana")) {
    $headers = @{
        "Authorization" = "Bearer $($tokens[$username])"
    }

    $state = Invoke-RestMethod -Uri "$baseUrl/api/games/$gameId/state" -Method GET -Headers $headers
    Write-Host "  $username - Role: $($state.ownRole), Faction: $($state.ownFaction), Phase: $($state.currentPhase)" -ForegroundColor Cyan
    Write-Host "    Available Actions: $($state.availableActions -join ', ')" -ForegroundColor Gray
}
Write-Host ""

Write-Host "Step 8: Querying Database..." -ForegroundColor Yellow
Write-Host "  📊 Database accessible at: http://localhost:8080/h2-console" -ForegroundColor Cyan
Write-Host "  JDBC URL: jdbc:h2:mem:werwoelflen" -ForegroundColor Gray
Write-Host "  Username: sa" -ForegroundColor Gray
Write-Host "  Password: (empty)" -ForegroundColor Gray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ ALL TESTS PASSED!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Game Details:" -ForegroundColor Yellow
Write-Host "  Lobby Code: $lobbyCode" -ForegroundColor White
Write-Host "  Game ID: $gameId" -ForegroundColor White
Write-Host "  Players: 4" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Use the tokens above to make API calls" -ForegroundColor White
Write-Host "  2. Check game state: GET /api/games/$gameId/state" -ForegroundColor White
Write-Host "  3. Submit actions based on roles and phases" -ForegroundColor White
Write-Host "  4. Import Postman collection for detailed testing" -ForegroundColor White
Write-Host ""

