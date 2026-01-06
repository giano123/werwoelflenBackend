package com.ausganslage.ausgangslageBackend.service;

import com.ausganslage.ausgangslageBackend.dto.*;
import com.ausganslage.ausgangslageBackend.enums.*;
import com.ausganslage.ausgangslageBackend.model.*;
import com.ausganslage.ausgangslageBackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GamePlayerRepository gamePlayerRepository;
    @Mock
    private GameActionRepository gameActionRepository;
    @Mock
    private LobbyRepository lobbyRepository;
    @Mock
    private LobbyMemberRepository lobbyMemberRepository;
    @Mock
    private RoleTemplateRepository roleTemplateRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private GameService gameService;

    private User hostUser;
    private Lobby lobby;
    private List<LobbyMember> lobbyMembers;
    private RoleTemplate werewolfRole;
    private RoleTemplate seerRole;
    private RoleTemplate witchRole;
    private RoleTemplate hunterRole;
    private RoleTemplate villagerRole;

    @BeforeEach
    void setUp() {
        hostUser = new User();
        hostUser.setId(1L);
        hostUser.setUsername("Host");

        lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("TEST123");
        lobby.setHostUserId(1L);
        lobby.setStatus(LobbyStatus.OPEN);

        lobbyMembers = new ArrayList<>();
        for (long i = 1; i <= 4; i++) {
            LobbyMember member = new LobbyMember();
            member.setId(i);
            member.setLobbyId(1L);
            member.setUserId(i);
            member.setIsReady(true);
            lobbyMembers.add(member);
        }

        werewolfRole = new RoleTemplate();
        werewolfRole.setId(1L);
        werewolfRole.setName(RoleName.WEREWOLF);
        werewolfRole.setFaction(Faction.WOLVES);

        seerRole = new RoleTemplate();
        seerRole.setId(2L);
        seerRole.setName(RoleName.SEER);
        seerRole.setFaction(Faction.VILLAGE);

        witchRole = new RoleTemplate();
        witchRole.setId(3L);
        witchRole.setName(RoleName.WITCH);
        witchRole.setFaction(Faction.VILLAGE);

        hunterRole = new RoleTemplate();
        hunterRole.setId(4L);
        hunterRole.setName(RoleName.HUNTER);
        hunterRole.setFaction(Faction.VILLAGE);

        villagerRole = new RoleTemplate();
        villagerRole.setId(5L);
        villagerRole.setName(RoleName.VILLAGER);
        villagerRole.setFaction(Faction.VILLAGE);
    }

    @Test
    void testStartGame_Success() {
        when(lobbyRepository.findByLobbyCode("TEST123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyId(1L)).thenReturn(lobbyMembers);
        when(roleTemplateRepository.findByName(RoleName.WEREWOLF)).thenReturn(Optional.of(werewolfRole));
        when(roleTemplateRepository.findByName(RoleName.SEER)).thenReturn(Optional.of(seerRole));
        when(roleTemplateRepository.findByName(RoleName.WITCH)).thenReturn(Optional.of(witchRole));
        when(roleTemplateRepository.findByName(RoleName.HUNTER)).thenReturn(Optional.of(hunterRole));
        when(roleTemplateRepository.findByName(RoleName.VILLAGER)).thenReturn(Optional.of(villagerRole));

        Game savedGame = new Game();
        savedGame.setId(1L);
        savedGame.setStatus(GameStatus.RUNNING);
        savedGame.setCurrentPhase(GamePhase.NIGHT_WOLVES);
        when(gameRepository.save(any(Game.class))).thenReturn(savedGame);

        Game result = gameService.startGame("TEST123", hostUser);

        assertNotNull(result);
        assertEquals(GameStatus.RUNNING, result.getStatus());
        assertEquals(GamePhase.NIGHT_WOLVES, result.getCurrentPhase());
        assertEquals(1, result.getDayNumber());
        verify(gamePlayerRepository, times(4)).save(any(GamePlayer.class));
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testStartGame_NotHost_ThrowsException() {
        User nonHost = new User();
        nonHost.setId(2L);

        when(lobbyRepository.findByLobbyCode("TEST123")).thenReturn(Optional.of(lobby));

        assertThrows(IllegalStateException.class, () ->
            gameService.startGame("TEST123", nonHost)
        );
    }

    @Test
    void testStartGame_NotEnoughPlayers_ThrowsException() {
        when(lobbyRepository.findByLobbyCode("TEST123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyId(1L)).thenReturn(lobbyMembers.subList(0, 3));

        assertThrows(IllegalStateException.class, () ->
            gameService.startGame("TEST123", hostUser)
        );
    }

    @Test
    void testStartGame_PlayersNotReady_ThrowsException() {
        lobbyMembers.get(0).setIsReady(false);

        when(lobbyRepository.findByLobbyCode("TEST123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyId(1L)).thenReturn(lobbyMembers);

        assertThrows(IllegalStateException.class, () ->
            gameService.startGame("TEST123", hostUser)
        );
    }

    @Test
    void testSubmitVote_WerewolfKill_Success() {
        Game game = createRunningGame();
        GamePlayer werewolf = createGamePlayer(1L, 1L, werewolfRole.getId(), true);
        GamePlayer victim = createGamePlayer(2L, 2L, villagerRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(werewolf));
        when(roleTemplateRepository.findById(werewolfRole.getId())).thenReturn(Optional.of(werewolfRole));
        when(gamePlayerRepository.findById(2L)).thenReturn(Optional.of(victim));
        when(roleTemplateRepository.findById(villagerRole.getId())).thenReturn(Optional.of(villagerRole));
        when(gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActorPlayerId(anyLong(), anyInt(), any(), anyLong()))
            .thenReturn(Optional.empty());
        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true)).thenReturn(List.of(werewolf, victim));

        VoteActionRequest request = new VoteActionRequest();
        request.setTargetPlayerId(2L);

        gameService.submitVote(1L, hostUser, request);

        verify(gameActionRepository, times(1)).save(any(GameAction.class));
    }

    @Test
    void testSubmitVote_WerewolfCannotKillWerewolf() {
        Game game = createRunningGame();
        GamePlayer werewolf1 = createGamePlayer(1L, 1L, werewolfRole.getId(), true);
        GamePlayer werewolf2 = createGamePlayer(2L, 2L, werewolfRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(werewolf1));
        when(roleTemplateRepository.findById(werewolfRole.getId())).thenReturn(Optional.of(werewolfRole));
        when(gamePlayerRepository.findById(2L)).thenReturn(Optional.of(werewolf2));

        VoteActionRequest request = new VoteActionRequest();
        request.setTargetPlayerId(2L);

        assertThrows(IllegalArgumentException.class, () ->
            gameService.submitVote(1L, hostUser, request)
        );
    }

    @Test
    void testSubmitVote_DeadPlayerCannotVote() {
        Game game = createRunningGame();
        GamePlayer deadPlayer = createGamePlayer(1L, 1L, villagerRole.getId(), false);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(deadPlayer));

        VoteActionRequest request = new VoteActionRequest();
        request.setTargetPlayerId(2L);

        assertThrows(IllegalStateException.class, () ->
            gameService.submitVote(1L, hostUser, request)
        );
    }

    @Test
    void testSubmitPowerAction_SeerInspect_Success() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.NIGHT_SEER);

        GamePlayer seer = createGamePlayer(1L, 1L, seerRole.getId(), true);
        GamePlayer target = createGamePlayer(2L, 2L, werewolfRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(seer));
        when(roleTemplateRepository.findById(seerRole.getId())).thenReturn(Optional.of(seerRole));
        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true)).thenReturn(List.of(seer, target));

        PowerActionRequest request = new PowerActionRequest();
        request.setActionType(ActionType.SEER_INSPECT);
        request.setTargetPlayerId(2L);

        gameService.submitPowerAction(1L, hostUser, request);

        verify(gameActionRepository, times(1)).save(any(GameAction.class));
    }

    @Test
    void testSubmitPowerAction_WitchHeal_Success() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.NIGHT_WITCH);

        GamePlayer witch = createGamePlayer(1L, 1L, witchRole.getId(), true);
        witch.setStateFlagsJson("{\"healPotion\":true,\"poisonPotion\":true}");

        GamePlayer victim = createGamePlayer(2L, 2L, villagerRole.getId(), true);

        GameAction wolfVote = new GameAction();
        wolfVote.setGameId(1L);
        wolfVote.setDayNumber(1);
        wolfVote.setPhase(GamePhase.NIGHT_WOLVES);
        wolfVote.setActionType(ActionType.VOTE_WOLF_KILL);
        wolfVote.setTargetPlayerId(2L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(witch));
        when(roleTemplateRepository.findById(witchRole.getId())).thenReturn(Optional.of(witchRole));
        when(gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
            1L, 1, GamePhase.NIGHT_WOLVES, ActionType.VOTE_WOLF_KILL))
            .thenReturn(List.of(wolfVote));
        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true)).thenReturn(List.of(witch));

        PowerActionRequest request = new PowerActionRequest();
        request.setActionType(ActionType.WITCH_HEAL);
        request.setTargetPlayerId(2L);

        gameService.submitPowerAction(1L, hostUser, request);

        verify(gameActionRepository, times(1)).save(any(GameAction.class));
        verify(gamePlayerRepository, times(1)).save(argThat(p ->
            p.getStateFlagsJson().contains("\"healPotion\":false")
        ));
    }

    @Test
    void testSubmitPowerAction_WitchHealAlreadyUsed_ThrowsException() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.NIGHT_WITCH);

        GamePlayer witch = createGamePlayer(1L, 1L, witchRole.getId(), true);
        witch.setStateFlagsJson("{\"healPotion\":false,\"poisonPotion\":true}");

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(witch));
        when(roleTemplateRepository.findById(witchRole.getId())).thenReturn(Optional.of(witchRole));

        PowerActionRequest request = new PowerActionRequest();
        request.setActionType(ActionType.WITCH_HEAL);
        request.setTargetPlayerId(2L);

        assertThrows(IllegalStateException.class, () ->
            gameService.submitPowerAction(1L, hostUser, request)
        );
    }

    @Test
    void testSubmitPowerAction_HunterShoot_Success() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.DAY_DISCUSSION);

        GamePlayer hunter = createGamePlayer(1L, 1L, hunterRole.getId(), false);
        hunter.setStateFlagsJson("{\"hunterShotAvailable\":true}");

        GamePlayer target = createGamePlayer(2L, 2L, villagerRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(hunter));
        when(roleTemplateRepository.findById(hunterRole.getId())).thenReturn(Optional.of(hunterRole));
        when(gamePlayerRepository.findById(2L)).thenReturn(Optional.of(target));
        when(roleTemplateRepository.findById(villagerRole.getId())).thenReturn(Optional.of(villagerRole));
        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true)).thenReturn(List.of(target));

        PowerActionRequest request = new PowerActionRequest();
        request.setActionType(ActionType.HUNTER_SHOOT);
        request.setTargetPlayerId(2L);

        gameService.submitPowerAction(1L, hostUser, request);

        verify(gameActionRepository, times(1)).save(any(GameAction.class));
        verify(gamePlayerRepository, atLeastOnce()).save(argThat(p ->
            !p.getIsAlive() && p.getId().equals(2L)
        ));
    }

    @Test
    void testSkipAction_Seer_Success() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.NIGHT_SEER);

        GamePlayer seer = createGamePlayer(1L, 1L, seerRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(seer));
        when(roleTemplateRepository.findById(seerRole.getId())).thenReturn(Optional.of(seerRole));
        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true)).thenReturn(List.of(seer));

        gameService.skipAction(1L, hostUser);

        verify(gameActionRepository, times(1)).save(argThat(action ->
            action.getPayloadJson().contains("skipped")
        ));
    }

    @Test
    void testCheckWinCondition_VillageWins() {
        Game game = createRunningGame();
        GamePlayer seer = createGamePlayer(1L, 1L, seerRole.getId(), true);
        GamePlayer witch = createGamePlayer(2L, 2L, witchRole.getId(), true);

        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true)).thenReturn(List.of(seer, witch));
        when(roleTemplateRepository.findById(seerRole.getId())).thenReturn(Optional.of(seerRole));
        when(roleTemplateRepository.findById(witchRole.getId())).thenReturn(Optional.of(witchRole));

        gameService.checkWinCondition(game);

        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(Faction.VILLAGE, game.getWinnerFaction());
        assertEquals(GamePhase.RESULT, game.getCurrentPhase());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testCheckWinCondition_WolvesWin() {
        Game game = createRunningGame();
        GamePlayer werewolf = createGamePlayer(1L, 1L, werewolfRole.getId(), true);
        GamePlayer villager = createGamePlayer(2L, 2L, villagerRole.getId(), true);

        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true)).thenReturn(List.of(werewolf, villager));
        when(roleTemplateRepository.findById(werewolfRole.getId())).thenReturn(Optional.of(werewolfRole));
        when(roleTemplateRepository.findById(villagerRole.getId())).thenReturn(Optional.of(villagerRole));

        gameService.checkWinCondition(game);

        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(Faction.WOLVES, game.getWinnerFaction());
        assertEquals(GamePhase.RESULT, game.getCurrentPhase());
    }

    @Test
    void testCheckWinCondition_GameContinues() {
        Game game = createRunningGame();
        GamePlayer werewolf = createGamePlayer(1L, 1L, werewolfRole.getId(), true);
        GamePlayer seer = createGamePlayer(2L, 2L, seerRole.getId(), true);
        GamePlayer witch = createGamePlayer(3L, 3L, witchRole.getId(), true);

        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true)).thenReturn(List.of(werewolf, seer, witch));
        when(roleTemplateRepository.findById(werewolfRole.getId())).thenReturn(Optional.of(werewolfRole));
        when(roleTemplateRepository.findById(seerRole.getId())).thenReturn(Optional.of(seerRole));
        when(roleTemplateRepository.findById(witchRole.getId())).thenReturn(Optional.of(witchRole));

        gameService.checkWinCondition(game);

        assertEquals(GameStatus.RUNNING, game.getStatus());
        assertNull(game.getWinnerFaction());
    }

    @Test
    void testTransitionToVoting_Success() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.DAY_DISCUSSION);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        gameService.transitionToVoting(1L);

        assertEquals(GamePhase.DAY_VOTING, game.getCurrentPhase());
        verify(gameRepository, times(1)).save(game);
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testTransitionToVoting_WrongPhase_ThrowsException() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.NIGHT_WOLVES);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThrows(IllegalStateException.class, () ->
            gameService.transitionToVoting(1L)
        );
    }

    @Test
    void testGetGameState_ReturnsCorrectRoleVisibility() {
        Game game = createRunningGame();
        GamePlayer werewolf1 = createGamePlayer(1L, 1L, werewolfRole.getId(), true);
        GamePlayer werewolf2 = createGamePlayer(2L, 2L, werewolfRole.getId(), true);
        GamePlayer seer = createGamePlayer(3L, 3L, seerRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(werewolf1));
        when(roleTemplateRepository.findById(werewolfRole.getId())).thenReturn(Optional.of(werewolfRole));
        when(gamePlayerRepository.findByGameId(1L)).thenReturn(List.of(werewolf1, werewolf2, seer));
        when(roleTemplateRepository.findById(seerRole.getId())).thenReturn(Optional.of(seerRole));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(hostUser));

        GameStateDto state = gameService.getGameState(1L, hostUser);

        assertNotNull(state);
        assertEquals(RoleName.WEREWOLF, state.getOwnRole());
        assertEquals(3, state.getPlayers().size());

        PlayerInfoDto werewolf2Info = state.getPlayers().stream()
            .filter(p -> p.getPlayerId().equals(2L))
            .findFirst()
            .orElse(null);
        assertNotNull(werewolf2Info);
        assertEquals(RoleName.WEREWOLF, werewolf2Info.getRole());

        PlayerInfoDto seerInfo = state.getPlayers().stream()
            .filter(p -> p.getPlayerId().equals(3L))
            .findFirst()
            .orElse(null);
        assertNotNull(seerInfo);
        assertNull(seerInfo.getRole());
    }

    @Test
    void testGetWolfVictim_OnlyWitchCanSee() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.NIGHT_WITCH);

        GamePlayer seer = createGamePlayer(1L, 1L, seerRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(seer));
        when(roleTemplateRepository.findById(seerRole.getId())).thenReturn(Optional.of(seerRole));

        assertThrows(IllegalStateException.class, () ->
            gameService.getWolfVictim(1L, hostUser)
        );
    }

    @Test
    void testSendChatMessage_DayPhase_Success() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.DAY_DISCUSSION);

        GamePlayer player = createGamePlayer(1L, 1L, villagerRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(player));
        when(roleTemplateRepository.findById(villagerRole.getId())).thenReturn(Optional.of(villagerRole));
        when(userRepository.findById(1L)).thenReturn(Optional.of(hostUser));

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(1L);
        savedMessage.setContent("Test message");
        savedMessage.setSenderUserId(1L);
        savedMessage.setChannel(ChatChannel.DAY);
        savedMessage.setCreatedAt(Instant.now());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Test message");

        gameService.sendChatMessage(1L, hostUser, request);

        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testSendChatMessage_WrongPhase_ThrowsException() {
        Game game = createRunningGame();
        game.setCurrentPhase(GamePhase.NIGHT_SEER);

        GamePlayer player = createGamePlayer(1L, 1L, villagerRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(player));
        when(roleTemplateRepository.findById(villagerRole.getId())).thenReturn(Optional.of(villagerRole));

        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Test message");

        assertThrows(IllegalStateException.class, () ->
            gameService.sendChatMessage(1L, hostUser, request)
        );
    }

    private Game createRunningGame() {
        Game game = new Game();
        game.setId(1L);
        game.setLobbyId(1L);
        game.setStatus(GameStatus.RUNNING);
        game.setCurrentPhase(GamePhase.NIGHT_WOLVES);
        game.setDayNumber(1);
        game.setCreatedAt(Instant.now());
        return game;
    }

    private GamePlayer createGamePlayer(Long id, Long userId, Long roleId, boolean isAlive) {
        GamePlayer player = new GamePlayer();
        player.setId(id);
        player.setGameId(1L);
        player.setUserId(userId);
        player.setRoleId(roleId);
        player.setSeatNumber(id.intValue());
        player.setIsAlive(isAlive);
        player.setRevealedRole(false);
        player.setStateFlagsJson("{}");
        return player;
    }
}

