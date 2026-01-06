package com.ausganslage.ausgangslageBackend.integration;

import com.ausganslage.ausgangslageBackend.dto.*;
import com.ausganslage.ausgangslageBackend.enums.*;
import com.ausganslage.ausgangslageBackend.model.*;
import com.ausganslage.ausgangslageBackend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private LobbyMemberRepository lobbyMemberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private RoleTemplateRepository roleTemplateRepository;

    private String user1Token;
    private String user2Token;
    private String user3Token;
    private String user4Token;

    @BeforeEach
    void setUp() throws Exception {
        user1Token = registerAndLogin("player1", "password123", "player1@test.com");
        user2Token = registerAndLogin("player2", "password123", "player2@test.com");
        user3Token = registerAndLogin("player3", "password123", "player3@test.com");
        user4Token = registerAndLogin("player4", "password123", "player4@test.com");
    }

    @Test
    void testCompleteGameFlow_VillageWins() throws Exception {
        CreateLobbyRequest createRequest = new CreateLobbyRequest();
        createRequest.setMaxPlayers(4);

        String lobbyCode = mockMvc.perform(post("/api/lobbies/create")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyCode").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Lobby lobby = objectMapper.readValue(lobbyCode, Lobby.class);

        mockMvc.perform(post("/api/lobbies/" + lobby.getLobbyCode() + "/join")
                .header("Authorization", user2Token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lobbies/" + lobby.getLobbyCode() + "/join")
                .header("Authorization", user3Token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lobbies/" + lobby.getLobbyCode() + "/join")
                .header("Authorization", user4Token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lobbies/" + lobby.getLobbyCode() + "/ready")
                .header("Authorization", user2Token)
                .param("isReady", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lobbies/" + lobby.getLobbyCode() + "/ready")
                .header("Authorization", user3Token)
                .param("isReady", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lobbies/" + lobby.getLobbyCode() + "/ready")
                .header("Authorization", user4Token)
                .param("isReady", "true"))
                .andExpect(status().isOk());

        String gameResponse = mockMvc.perform(post("/api/lobbies/" + lobby.getLobbyCode() + "/start")
                .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.currentPhase").value("NIGHT_WOLVES"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Game game = objectMapper.readValue(gameResponse, Game.class);

        mockMvc.perform(get("/api/games/" + game.getId() + "/state")
                .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPhase").value("NIGHT_WOLVES"))
                .andExpect(jsonPath("$.players").isArray())
                .andExpect(jsonPath("$.players", hasSize(4)));
    }

    @Test
    void testAuthenticationFlow() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newplayer");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("new@test.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.username").value("newplayer"));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("newplayer");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.username").value("newplayer"));
    }

    @Test
    void testLobbyFlow() throws Exception {
        CreateLobbyRequest createRequest = new CreateLobbyRequest();
        createRequest.setMaxPlayers(6);

        String response = mockMvc.perform(post("/api/lobbies/create")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Lobby lobby = objectMapper.readValue(response, Lobby.class);

        mockMvc.perform(get("/api/lobbies/" + lobby.getLobbyCode())
                .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyCode").value(lobby.getLobbyCode()))
                .andExpect(jsonPath("$.members", hasSize(1)));

        mockMvc.perform(post("/api/lobbies/" + lobby.getLobbyCode() + "/join")
                .header("Authorization", user2Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lobbies/" + lobby.getLobbyCode())
                .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)));

        mockMvc.perform(post("/api/lobbies/" + lobby.getLobbyCode() + "/leave")
                .header("Authorization", user2Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lobbies/" + lobby.getLobbyCode())
                .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(1)));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(post("/api/lobbies/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/games/1/state"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String username, String password, String email) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);

        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        return authResponse.getToken();
    }
}

