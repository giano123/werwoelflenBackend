package com.ausganslage.ausgangslageBackend.service;

import com.ausganslage.ausgangslageBackend.dto.PowerActionRequest;
import com.ausganslage.ausgangslageBackend.dto.VoteActionRequest;
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
class GameServiceEdgeCasesTest {

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

    private User user;
    private Game game;
    private RoleTemplate werewolfRole;
    private RoleTemplate seerRole;
    private RoleTemplate witchRole;
    private RoleTemplate hunterRole;
    private RoleTemplate villagerRole;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("TestUser");

        game = new Game();
        game.setId(1L);
        game.setStatus(GameStatus.RUNNING);
        game.setCurrentPhase(GamePhase.NIGHT_WOLVES);
        game.setDayNumber(1);

        werewolfRole = createRole(1L, RoleName.WEREWOLF, Faction.WOLVES);
        seerRole = createRole(2L, RoleName.SEER, Faction.VILLAGE);
        witchRole = createRole(3L, RoleName.WITCH, Faction.VILLAGE);
        hunterRole = createRole(4L, RoleName.HUNTER, Faction.VILLAGE);
        villagerRole = createRole(5L, RoleName.VILLAGER, Faction.VILLAGE);
    }

    @Test
    void testWitchHealWrongTarget_ShouldFail() {
        game.setCurrentPhase(GamePhase.NIGHT_WITCH);

        GamePlayer witch = createPlayer(1L, witchRole.getId(), true);
        witch.setStateFlagsJson("{\"healPotion\":true,\"poisonPotion\":true}");

        GamePlayer wolfVictim = createPlayer(2L, villagerRole.getId(), true);
        GamePlayer otherPlayer = createPlayer(3L, villagerRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(witch));
        when(roleTemplateRepository.findById(witchRole.getId())).thenReturn(Optional.of(witchRole));

        List<GameAction> wolfVotes = List.of(
            createAction(1L, 2L, ActionType.VOTE_WOLF_KILL)
        );
        when(gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
            1L, 1, GamePhase.NIGHT_WOLVES, ActionType.VOTE_WOLF_KILL))
            .thenReturn(wolfVotes);

        PowerActionRequest request = new PowerActionRequest();
        request.setActionType(ActionType.WITCH_HEAL);
        request.setTargetPlayerId(3L);

        assertThrows(IllegalArgumentException.class, () ->
            gameService.submitPowerAction(1L, user, request)
        );
    }

    @Test
    void testTieVoteResolution() {
        game.setCurrentPhase(GamePhase.DAY_VOTING);

        GamePlayer player1 = createPlayer(1L, villagerRole.getId(), true);
        GamePlayer player2 = createPlayer(2L, villagerRole.getId(), true);
        GamePlayer player3 = createPlayer(3L, villagerRole.getId(), true);

        List<GameAction> votes = Arrays.asList(
            createAction(1L, 2L, ActionType.VOTE_LYNCH),
            createAction(3L, 1L, ActionType.VOTE_LYNCH)
        );

        when(gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
            1L, 1, GamePhase.DAY_VOTING, ActionType.VOTE_LYNCH))
            .thenReturn(votes);

        gameService.resolveDayVoting(game);

        verify(gamePlayerRepository, never()).save(argThat(p -> !p.getIsAlive()));
        verify(chatMessageRepository, times(1)).save(argThat(msg ->
            msg.getContent().contains("No one was lynched")
        ));
    }

    @Test
    void testHunterKillsHunter_ChainReaction() {
        game.setCurrentPhase(GamePhase.DAY_DISCUSSION);

        GamePlayer hunter1 = createPlayer(1L, hunterRole.getId(), false);
        hunter1.setStateFlagsJson("{\"hunterShotAvailable\":true}");

        GamePlayer hunter2 = createPlayer(2L, hunterRole.getId(), true);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(hunter1));
        when(roleTemplateRepository.findById(hunterRole.getId())).thenReturn(Optional.of(hunterRole));
        when(gamePlayerRepository.findById(2L)).thenReturn(Optional.of(hunter2));
        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true)).thenReturn(List.of(hunter2));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        PowerActionRequest request = new PowerActionRequest();
        request.setActionType(ActionType.HUNTER_SHOOT);
        request.setTargetPlayerId(2L);

        gameService.submitPowerAction(1L, user, request);

        verify(gamePlayerRepository, atLeastOnce()).save(argThat(p ->
            p.getId().equals(2L) && !p.getIsAlive()
        ));

        verify(gamePlayerRepository, atLeastOnce()).save(argThat(p ->
            p.getId().equals(2L) && p.getStateFlagsJson().contains("hunterShotAvailable")
        ));
    }

    @Test
    void testWolfKillsWitch_BeforeWitchPhase() {
        GamePlayer werewolf = createPlayer(1L, werewolfRole.getId(), true);
        GamePlayer witch = createPlayer(2L, witchRole.getId(), true);
        GamePlayer villager1 = createPlayer(3L, villagerRole.getId(), true);
        GamePlayer villager2 = createPlayer(4L, villagerRole.getId(), true);

        List<GameAction> wolfVotes = List.of(
            createAction(1L, 2L, ActionType.VOTE_WOLF_KILL)
        );

        when(gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
            1L, 1, GamePhase.NIGHT_WOLVES, ActionType.VOTE_WOLF_KILL))
            .thenReturn(wolfVotes);
        when(gameActionRepository.findByGameIdAndDayNumberAndPhase(
            1L, 1, GamePhase.NIGHT_WITCH))
            .thenReturn(Collections.emptyList());
        when(gamePlayerRepository.findById(2L)).thenReturn(Optional.of(witch));
        when(roleTemplateRepository.findById(witchRole.getId())).thenReturn(Optional.of(witchRole));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        gameService.resolveNightActions(game);

        verify(gamePlayerRepository, atLeastOnce()).save(argThat(p ->
            p.getId().equals(2L) && !p.getIsAlive()
        ));
    }

    @Test
    void testNoWolvesLeft_VillageWins() {
        GamePlayer seer = createPlayer(1L, seerRole.getId(), true);
        GamePlayer witch = createPlayer(2L, witchRole.getId(), true);
        GamePlayer hunter = createPlayer(3L, hunterRole.getId(), true);

        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true))
            .thenReturn(List.of(seer, witch, hunter));
        when(roleTemplateRepository.findById(seerRole.getId())).thenReturn(Optional.of(seerRole));
        when(roleTemplateRepository.findById(witchRole.getId())).thenReturn(Optional.of(witchRole));
        when(roleTemplateRepository.findById(hunterRole.getId())).thenReturn(Optional.of(hunterRole));

        gameService.checkWinCondition(game);

        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(Faction.VILLAGE, game.getWinnerFaction());
        assertEquals(GamePhase.RESULT, game.getCurrentPhase());
    }

    @Test
    void testEqualWolvesAndVillagers_WolvesWin() {
        GamePlayer werewolf = createPlayer(1L, werewolfRole.getId(), true);
        GamePlayer seer = createPlayer(2L, seerRole.getId(), true);

        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true))
            .thenReturn(List.of(werewolf, seer));
        when(roleTemplateRepository.findById(werewolfRole.getId())).thenReturn(Optional.of(werewolfRole));
        when(roleTemplateRepository.findById(seerRole.getId())).thenReturn(Optional.of(seerRole));

        gameService.checkWinCondition(game);

        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(Faction.WOLVES, game.getWinnerFaction());
    }

    @Test
    void testMoreWolvesThanVillagers_WolvesWin() {
        GamePlayer werewolf1 = createPlayer(1L, werewolfRole.getId(), true);
        GamePlayer werewolf2 = createPlayer(2L, werewolfRole.getId(), true);
        GamePlayer seer = createPlayer(3L, seerRole.getId(), true);

        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true))
            .thenReturn(List.of(werewolf1, werewolf2, seer));
        when(roleTemplateRepository.findById(werewolfRole.getId())).thenReturn(Optional.of(werewolfRole));
        when(roleTemplateRepository.findById(seerRole.getId())).thenReturn(Optional.of(seerRole));

        gameService.checkWinCondition(game);

        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(Faction.WOLVES, game.getWinnerFaction());
    }

    @Test
    void testVoteRevoting_ReplacesOldVote() {
        GamePlayer werewolf = createPlayer(1L, werewolfRole.getId(), true);
        GamePlayer victim1 = createPlayer(2L, villagerRole.getId(), true);
        GamePlayer victim2 = createPlayer(3L, villagerRole.getId(), true);

        GameAction oldVote = createAction(1L, 2L, ActionType.VOTE_WOLF_KILL);
        oldVote.setId(100L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByGameIdAndUserId(1L, 1L)).thenReturn(Optional.of(werewolf));
        when(roleTemplateRepository.findById(werewolfRole.getId())).thenReturn(Optional.of(werewolfRole));
        when(gamePlayerRepository.findById(3L)).thenReturn(Optional.of(victim2));
        when(roleTemplateRepository.findById(villagerRole.getId())).thenReturn(Optional.of(villagerRole));
        when(gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActorPlayerId(
            1L, 1, GamePhase.NIGHT_WOLVES, 1L))
            .thenReturn(Optional.of(oldVote));
        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true))
            .thenReturn(List.of(werewolf, victim1, victim2));

        VoteActionRequest request = new VoteActionRequest();
        request.setTargetPlayerId(3L);

        gameService.submitVote(1L, user, request);

        verify(gameActionRepository, times(1)).delete(oldVote);
        verify(gameActionRepository, times(1)).save(any(GameAction.class));
    }

    @Test
    void testDeadSeer_PhaseAdvancesAutomatically() {
        game.setCurrentPhase(GamePhase.NIGHT_SEER);

        GamePlayer deadSeer = createPlayer(1L, seerRole.getId(), false);
        GamePlayer werewolf = createPlayer(2L, werewolfRole.getId(), true);
        GamePlayer villager1 = createPlayer(3L, villagerRole.getId(), true);
        GamePlayer villager2 = createPlayer(4L, villagerRole.getId(), true);

        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true))
            .thenReturn(List.of(werewolf, villager1, villager2));
        when(roleTemplateRepository.findById(werewolfRole.getId()))
            .thenReturn(Optional.of(werewolfRole));
        when(roleTemplateRepository.findById(villagerRole.getId()))
            .thenReturn(Optional.of(villagerRole));

        gameService.checkAndAdvancePhase(game);

        assertEquals(GamePhase.NIGHT_WITCH, game.getCurrentPhase());
    }

    @Test
    void testDeadWitch_PhaseAdvancesAutomatically() {
        game.setCurrentPhase(GamePhase.NIGHT_WITCH);

        GamePlayer deadWitch = createPlayer(1L, witchRole.getId(), false);
        GamePlayer werewolf = createPlayer(2L, werewolfRole.getId(), true);
        GamePlayer villager1 = createPlayer(3L, villagerRole.getId(), true);
        GamePlayer villager2 = createPlayer(4L, villagerRole.getId(), true);

        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true))
            .thenReturn(List.of(werewolf, villager1, villager2));
        when(roleTemplateRepository.findById(werewolfRole.getId()))
            .thenReturn(Optional.of(werewolfRole));
        when(roleTemplateRepository.findById(villagerRole.getId()))
            .thenReturn(Optional.of(villagerRole));
        when(gameActionRepository.findByGameIdAndDayNumberAndPhase(
            anyLong(), anyInt(), any()))
            .thenReturn(Collections.emptyList());

        gameService.checkAndAdvancePhase(game);

        assertEquals(GamePhase.DAY_DISCUSSION, game.getCurrentPhase());
    }

    @Test
    void testWitchWithNoPotions_CannotAct() {
        game.setCurrentPhase(GamePhase.NIGHT_WITCH);

        GamePlayer witch = createPlayer(1L, witchRole.getId(), true);
        witch.setStateFlagsJson("{\"healPotion\":false,\"poisonPotion\":false}");
        GamePlayer werewolf = createPlayer(2L, werewolfRole.getId(), true);
        GamePlayer villager1 = createPlayer(3L, villagerRole.getId(), true);
        GamePlayer villager2 = createPlayer(4L, villagerRole.getId(), true);

        when(gamePlayerRepository.findByGameIdAndIsAlive(1L, true))
            .thenReturn(List.of(witch, werewolf, villager1, villager2));
        when(roleTemplateRepository.findById(witchRole.getId()))
            .thenReturn(Optional.of(witchRole));
        when(roleTemplateRepository.findById(werewolfRole.getId()))
            .thenReturn(Optional.of(werewolfRole));
        when(roleTemplateRepository.findById(villagerRole.getId()))
            .thenReturn(Optional.of(villagerRole));
        when(gameActionRepository.findByGameIdAndDayNumberAndPhase(
            1L, 1, GamePhase.NIGHT_WITCH))
            .thenReturn(Collections.emptyList());

        gameService.checkAndAdvancePhase(game);

        assertEquals(GamePhase.DAY_DISCUSSION, game.getCurrentPhase());
    }

    private RoleTemplate createRole(Long id, RoleName name, Faction faction) {
        RoleTemplate role = new RoleTemplate();
        role.setId(id);
        role.setName(name);
        role.setFaction(faction);
        return role;
    }

    private GamePlayer createPlayer(Long id, Long roleId, boolean isAlive) {
        GamePlayer player = new GamePlayer();
        player.setId(id);
        player.setGameId(1L);
        player.setUserId(id);
        player.setRoleId(roleId);
        player.setIsAlive(isAlive);
        player.setRevealedRole(false);
        player.setStateFlagsJson("{}");
        return player;
    }

    private GameAction createAction(Long actorId, Long targetId, ActionType type) {
        GameAction action = new GameAction();
        action.setActorPlayerId(actorId);
        action.setTargetPlayerId(targetId);
        action.setActionType(type);
        action.setGameId(1L);
        action.setDayNumber(1);
        action.setPhase(game.getCurrentPhase());
        action.setCreatedAt(Instant.now());
        return action;
    }
}

