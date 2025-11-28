package com.ausganslage.ausgangslageBackend.service;

import com.ausganslage.ausgangslageBackend.dto.*;
import com.ausganslage.ausgangslageBackend.enums.*;
import com.ausganslage.ausgangslageBackend.model.*;
import com.ausganslage.ausgangslageBackend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final GameActionRepository gameActionRepository;
    private final LobbyRepository lobbyRepository;
    private final LobbyMemberRepository lobbyMemberRepository;
    private final RoleTemplateRepository roleTemplateRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameService(GameRepository gameRepository, GamePlayerRepository gamePlayerRepository,
                       GameActionRepository gameActionRepository, LobbyRepository lobbyRepository,
                       LobbyMemberRepository lobbyMemberRepository, RoleTemplateRepository roleTemplateRepository,
                       UserRepository userRepository, ChatMessageRepository chatMessageRepository) {
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.gameActionRepository = gameActionRepository;
        this.lobbyRepository = lobbyRepository;
        this.lobbyMemberRepository = lobbyMemberRepository;
        this.roleTemplateRepository = roleTemplateRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public Game startGame(String lobbyCode, User currentUser) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode)
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found"));

        if (!lobby.getHostUserId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the host can start the game");
        }

        if (lobby.getStatus() != LobbyStatus.OPEN) {
            throw new IllegalStateException("Lobby is not open");
        }

        List<LobbyMember> members = lobbyMemberRepository.findByLobbyId(lobby.getId());

        if (members.size() < 4) {
            throw new IllegalStateException("Need at least 4 players to start");
        }

        long notReady = members.stream().filter(m -> !m.getIsReady()).count();
        if (notReady > 0) {
            throw new IllegalStateException("All players must be ready");
        }

        Game game = new Game();
        game.setLobbyId(lobby.getId());
        game.setStatus(GameStatus.RUNNING);
        game.setCurrentPhase(GamePhase.NIGHT_WOLVES);
        game.setDayNumber(1);
        game.setCreatedAt(Instant.now());

        game = gameRepository.save(game);

        distributeRoles(game, members);

        lobby.setStatus(LobbyStatus.IN_GAME);
        lobbyRepository.save(lobby);

        createSystemMessage(game.getId(), "Game started! Night falls...");

        return game;
    }

    private void distributeRoles(Game game, List<LobbyMember> members) {
        int playerCount = members.size();
        List<RoleTemplate> rolesToAssign = new ArrayList<>();

        int werewolfCount = Math.max(1, playerCount / 4);
        RoleTemplate werewolf = roleTemplateRepository.findByName(RoleName.WEREWOLF)
                .orElseThrow(() -> new IllegalStateException("Werewolf role not found"));
        for (int i = 0; i < werewolfCount; i++) {
            rolesToAssign.add(werewolf);
        }

        RoleTemplate seer = roleTemplateRepository.findByName(RoleName.SEER)
                .orElseThrow(() -> new IllegalStateException("Seer role not found"));
        rolesToAssign.add(seer);

        RoleTemplate witch = roleTemplateRepository.findByName(RoleName.WITCH)
                .orElseThrow(() -> new IllegalStateException("Witch role not found"));
        rolesToAssign.add(witch);

        RoleTemplate hunter = roleTemplateRepository.findByName(RoleName.HUNTER)
                .orElseThrow(() -> new IllegalStateException("Hunter role not found"));
        rolesToAssign.add(hunter);

        RoleTemplate villager = roleTemplateRepository.findByName(RoleName.VILLAGER)
                .orElseThrow(() -> new IllegalStateException("Villager role not found"));
        while (rolesToAssign.size() < playerCount) {
            rolesToAssign.add(villager);
        }

        Collections.shuffle(rolesToAssign);

        List<LobbyMember> shuffledMembers = new ArrayList<>(members);
        Collections.shuffle(shuffledMembers);

        for (int i = 0; i < shuffledMembers.size(); i++) {
            LobbyMember member = shuffledMembers.get(i);
            RoleTemplate role = rolesToAssign.get(i);

            GamePlayer player = new GamePlayer();
            player.setGameId(game.getId());
            player.setUserId(member.getUserId());
            player.setRoleId(role.getId());
            player.setSeatNumber(i + 1);
            player.setIsAlive(true);
            player.setRevealedRole(false);

            Map<String, Object> stateFlags = new HashMap<>();
            if (role.getName() == RoleName.WITCH) {
                stateFlags.put("healPotion", true);
                stateFlags.put("poisonPotion", true);
            } else if (role.getName() == RoleName.HUNTER) {
                stateFlags.put("hunterShotAvailable", true);
            }
            player.setStateFlagsJson(toJson(stateFlags));

            gamePlayerRepository.save(player);
        }
    }

    @Transactional(readOnly = true)
    public GameStateDto getGameState(Long gameId, User currentUser) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not in this game"));

        RoleTemplate currentRole = roleTemplateRepository.findById(currentPlayer.getRoleId())
                .orElseThrow(() -> new IllegalStateException("Role not found"));

        GameStateDto dto = new GameStateDto();
        dto.setGameId(game.getId());
        dto.setStatus(game.getStatus());
        dto.setCurrentPhase(game.getCurrentPhase());
        dto.setDayNumber(game.getDayNumber());
        dto.setWinnerFaction(game.getWinnerFaction());

        dto.setOwnRole(currentRole.getName());
        dto.setOwnFaction(currentRole.getFaction());
        dto.setIsAlive(currentPlayer.getIsAlive());
        dto.setOwnStateFlags(fromJson(currentPlayer.getStateFlagsJson()));

        List<GamePlayer> allPlayers = gamePlayerRepository.findByGameId(gameId);
        dto.setPlayers(allPlayers.stream()
                .map(p -> toPlayerInfoDto(p, currentPlayer, currentRole))
                .collect(Collectors.toList()));

        dto.setAvailableActions(calculateAvailableActions(game, currentPlayer, currentRole));
        dto.setPhaseDescription(getPhaseDescription(game.getCurrentPhase()));

        return dto;
    }

    @Transactional
    public void submitVote(Long gameId, User currentUser, VoteActionRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not in this game"));

        if (!currentPlayer.getIsAlive()) {
            throw new IllegalStateException("Dead players cannot vote");
        }

        RoleTemplate currentRole = roleTemplateRepository.findById(currentPlayer.getRoleId())
                .orElseThrow(() -> new IllegalStateException("Role not found"));

        ActionType actionType;
        if (game.getCurrentPhase() == GamePhase.DAY_VOTING) {
            actionType = ActionType.VOTE_LYNCH;
        } else if (game.getCurrentPhase() == GamePhase.NIGHT_WOLVES && currentRole.getName() == RoleName.WEREWOLF) {
            actionType = ActionType.VOTE_WOLF_KILL;
        } else {
            throw new IllegalStateException("Invalid voting phase");
        }

        GamePlayer targetPlayer = gamePlayerRepository.findById(request.getTargetPlayerId())
                .orElseThrow(() -> new IllegalArgumentException("Target player not found"));

        if (!targetPlayer.getIsAlive()) {
            throw new IllegalArgumentException("Cannot vote for dead player");
        }

        if (actionType == ActionType.VOTE_WOLF_KILL && currentRole.getName() == RoleName.WEREWOLF) {
            RoleTemplate targetRole = roleTemplateRepository.findById(targetPlayer.getRoleId()).orElse(null);
            if (targetRole != null && targetRole.getName() == RoleName.WEREWOLF) {
                throw new IllegalArgumentException("Werewolves cannot kill each other");
            }
        }

        Optional<GameAction> existingVote = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActorPlayerId(
                gameId, game.getDayNumber(), game.getCurrentPhase(), currentPlayer.getId());

        existingVote.ifPresent(gameActionRepository::delete);

        GameAction action = new GameAction();
        action.setGameId(gameId);
        action.setDayNumber(game.getDayNumber());
        action.setPhase(game.getCurrentPhase());
        action.setActorPlayerId(currentPlayer.getId());
        action.setTargetPlayerId(targetPlayer.getId());
        action.setActionType(actionType);
        action.setCreatedAt(Instant.now());

        gameActionRepository.save(action);

        checkAndAdvancePhase(game);
    }

    @Transactional
    public void submitPowerAction(Long gameId, User currentUser, PowerActionRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not in this game"));

        if (!currentPlayer.getIsAlive() && request.getActionType() != ActionType.HUNTER_SHOOT) {
            throw new IllegalStateException("Dead players cannot use powers");
        }

        RoleTemplate currentRole = roleTemplateRepository.findById(currentPlayer.getRoleId())
                .orElseThrow(() -> new IllegalStateException("Role not found"));

        validatePowerAction(game, currentPlayer, currentRole, request);

        GameAction action = new GameAction();
        action.setGameId(gameId);
        action.setDayNumber(game.getDayNumber());
        action.setPhase(game.getCurrentPhase());
        action.setActorPlayerId(currentPlayer.getId());
        action.setTargetPlayerId(request.getTargetPlayerId());
        action.setActionType(request.getActionType());
        action.setCreatedAt(Instant.now());

        gameActionRepository.save(action);

        if (request.getActionType() == ActionType.WITCH_HEAL || request.getActionType() == ActionType.WITCH_POISON) {
            updateWitchPotions(currentPlayer, request.getActionType());
        }

        if (request.getActionType() == ActionType.HUNTER_SHOOT) {
            Map<String, Object> flags = fromJson(currentPlayer.getStateFlagsJson());
            flags.put("hunterShotAvailable", false);
            currentPlayer.setStateFlagsJson(toJson(flags));
            gamePlayerRepository.save(currentPlayer);

            GamePlayer target = gamePlayerRepository.findById(request.getTargetPlayerId())
                    .orElseThrow(() -> new IllegalArgumentException("Target not found"));
            killPlayer(target, game);
        }

        checkAndAdvancePhase(game);
    }

    @Transactional
    public void skipAction(Long gameId, User currentUser) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not in this game"));

        if (!currentPlayer.getIsAlive()) {
            throw new IllegalStateException("Dead players cannot skip");
        }

        RoleTemplate currentRole = roleTemplateRepository.findById(currentPlayer.getRoleId())
                .orElseThrow(() -> new IllegalStateException("Role not found"));

        if (game.getCurrentPhase() == GamePhase.NIGHT_SEER && currentRole.getName() == RoleName.SEER) {
            GameAction skipAction = new GameAction();
            skipAction.setGameId(gameId);
            skipAction.setDayNumber(game.getDayNumber());
            skipAction.setPhase(game.getCurrentPhase());
            skipAction.setActorPlayerId(currentPlayer.getId());
            skipAction.setActionType(ActionType.SEER_INSPECT);
            skipAction.setPayloadJson("{\"skipped\":true}");
            skipAction.setCreatedAt(Instant.now());
            gameActionRepository.save(skipAction);
        } else if (game.getCurrentPhase() == GamePhase.NIGHT_WITCH && currentRole.getName() == RoleName.WITCH) {
            GameAction skipAction = new GameAction();
            skipAction.setGameId(gameId);
            skipAction.setDayNumber(game.getDayNumber());
            skipAction.setPhase(game.getCurrentPhase());
            skipAction.setActorPlayerId(currentPlayer.getId());
            skipAction.setActionType(ActionType.WITCH_HEAL);
            skipAction.setPayloadJson("{\"skipped\":true}");
            skipAction.setCreatedAt(Instant.now());
            gameActionRepository.save(skipAction);
        } else {
            throw new IllegalStateException("Cannot skip during this phase");
        }

        checkAndAdvancePhase(game);
    }

    private void validatePowerAction(Game game, GamePlayer player, RoleTemplate role, PowerActionRequest request) {
        Map<String, Object> flags = fromJson(player.getStateFlagsJson());

        switch (request.getActionType()) {
            case SEER_INSPECT:
                if (role.getName() != RoleName.SEER) {
                    throw new IllegalStateException("Only the Seer can inspect");
                }
                if (game.getCurrentPhase() != GamePhase.NIGHT_SEER) {
                    throw new IllegalStateException("Can only inspect during Seer phase");
                }
                break;

            case WITCH_HEAL:
                if (role.getName() != RoleName.WITCH) {
                    throw new IllegalStateException("Only the Witch can heal");
                }
                if (game.getCurrentPhase() != GamePhase.NIGHT_WITCH) {
                    throw new IllegalStateException("Can only heal during Witch phase");
                }
                if (!Boolean.TRUE.equals(flags.get("healPotion"))) {
                    throw new IllegalStateException("Heal potion already used");
                }
                break;

            case WITCH_POISON:
                if (role.getName() != RoleName.WITCH) {
                    throw new IllegalStateException("Only the Witch can poison");
                }
                if (game.getCurrentPhase() != GamePhase.NIGHT_WITCH) {
                    throw new IllegalStateException("Can only poison during Witch phase");
                }
                if (!Boolean.TRUE.equals(flags.get("poisonPotion"))) {
                    throw new IllegalStateException("Poison potion already used");
                }
                break;

            case HUNTER_SHOOT:
                if (role.getName() != RoleName.HUNTER) {
                    throw new IllegalStateException("Only the Hunter can shoot");
                }
                if (!Boolean.TRUE.equals(flags.get("hunterShotAvailable"))) {
                    throw new IllegalStateException("Hunter shot not available");
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid power action type");
        }
    }

    private void updateWitchPotions(GamePlayer witch, ActionType actionType) {
        Map<String, Object> flags = fromJson(witch.getStateFlagsJson());
        if (actionType == ActionType.WITCH_HEAL) {
            flags.put("healPotion", false);
        } else if (actionType == ActionType.WITCH_POISON) {
            flags.put("poisonPotion", false);
        }
        witch.setStateFlagsJson(toJson(flags));
        gamePlayerRepository.save(witch);
    }

    @Transactional
    public void checkAndAdvancePhase(Game game) {
        List<GamePlayer> alivePlayers = gamePlayerRepository.findByGameIdAndIsAlive(game.getId(), true);

        switch (game.getCurrentPhase()) {
            case NIGHT_WOLVES:
                if (hasAllWerewolvesVoted(game, alivePlayers)) {
                    game.setCurrentPhase(GamePhase.NIGHT_SEER);
                    gameRepository.save(game);
                }
                break;

            case NIGHT_SEER:
                if (hasSeerActed(game, alivePlayers) || !hasSeerAlive(alivePlayers)) {
                    game.setCurrentPhase(GamePhase.NIGHT_WITCH);
                    gameRepository.save(game);
                }
                break;

            case NIGHT_WITCH:
                if (hasWitchActed(game, alivePlayers) || !hasWitchAlive(alivePlayers)) {
                    resolveNightActions(game);
                    game.setCurrentPhase(GamePhase.DAY_DISCUSSION);
                    gameRepository.save(game);
                }
                break;

            case DAY_DISCUSSION:
                break;

            case DAY_VOTING:
                if (hasAllAlivePlayersVoted(game, alivePlayers)) {
                    resolveDayVoting(game);
                }
                break;

            case RESULT:
                break;
        }

        checkWinCondition(game);
    }

    private boolean hasAllWerewolvesVoted(Game game, List<GamePlayer> alivePlayers) {
        List<GamePlayer> aliveWerewolves = alivePlayers.stream()
                .filter(p -> {
                    RoleTemplate role = roleTemplateRepository.findById(p.getRoleId()).orElse(null);
                    return role != null && role.getName() == RoleName.WEREWOLF;
                })
                .collect(Collectors.toList());

        if (aliveWerewolves.isEmpty()) return true;

        List<GameAction> votes = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_WOLVES, ActionType.VOTE_WOLF_KILL);

        return votes.size() >= aliveWerewolves.size();
    }

    private boolean hasSeerActed(Game game, List<GamePlayer> alivePlayers) {
        List<GameAction> actions = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_SEER, ActionType.SEER_INSPECT);
        return !actions.isEmpty();
    }

    private boolean hasSeerAlive(List<GamePlayer> alivePlayers) {
        return alivePlayers.stream().anyMatch(p -> {
            RoleTemplate role = roleTemplateRepository.findById(p.getRoleId()).orElse(null);
            return role != null && role.getName() == RoleName.SEER;
        });
    }

    private boolean hasWitchActed(Game game, List<GamePlayer> alivePlayers) {
        List<GameAction> actions = gameActionRepository.findByGameIdAndDayNumberAndPhase(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_WITCH);

        if (!actions.isEmpty()) {
            return true;
        }

        GamePlayer witch = alivePlayers.stream()
                .filter(p -> {
                    RoleTemplate role = roleTemplateRepository.findById(p.getRoleId()).orElse(null);
                    return role != null && role.getName() == RoleName.WITCH;
                })
                .findFirst()
                .orElse(null);

        if (witch == null) {
            return true;
        }

        Map<String, Object> flags = fromJson(witch.getStateFlagsJson());
        if (!Boolean.TRUE.equals(flags.get("healPotion")) && !Boolean.TRUE.equals(flags.get("poisonPotion"))) {
            return true;
        }

        return false;
    }

    private boolean hasWitchAlive(List<GamePlayer> alivePlayers) {
        return alivePlayers.stream().anyMatch(p -> {
            RoleTemplate role = roleTemplateRepository.findById(p.getRoleId()).orElse(null);
            return role != null && role.getName() == RoleName.WITCH;
        });
    }

    private boolean hasAllAlivePlayersVoted(Game game, List<GamePlayer> alivePlayers) {
        List<GameAction> votes = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.DAY_VOTING, ActionType.VOTE_LYNCH);

        return votes.size() >= alivePlayers.size();
    }

    @Transactional
    public void resolveNightActions(Game game) {
        List<GameAction> wolfVotes = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_WOLVES, ActionType.VOTE_WOLF_KILL);

        Long wolfVictimId = getMajorityTarget(wolfVotes);

        List<GameAction> witchActions = gameActionRepository.findByGameIdAndDayNumberAndPhase(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_WITCH);

        boolean healed = false;
        Long poisonedPlayerId = null;

        for (GameAction action : witchActions) {
            if (action.getActionType() == ActionType.WITCH_HEAL) {
                if (action.getTargetPlayerId().equals(wolfVictimId)) {
                    healed = true;
                    createSystemMessage(game.getId(), "The Witch saved someone from the wolves!");
                }
            } else if (action.getActionType() == ActionType.WITCH_POISON) {
                poisonedPlayerId = action.getTargetPlayerId();
            }
        }

        if (wolfVictimId != null && !healed) {
            GamePlayer victim = gamePlayerRepository.findById(wolfVictimId).orElse(null);
            if (victim != null) {
                killPlayer(victim, game);
                User victimUser = userRepository.findById(victim.getUserId()).orElse(null);
                String victimName = victimUser != null ? victimUser.getUsername() : "Unknown";
                createSystemMessage(game.getId(), victimName + " was killed by werewolves during the night!");
            }
        }

        if (poisonedPlayerId != null) {
            GamePlayer poisoned = gamePlayerRepository.findById(poisonedPlayerId).orElse(null);
            if (poisoned != null && poisoned.getIsAlive()) {
                killPlayer(poisoned, game);
                User poisonedUser = userRepository.findById(poisoned.getUserId()).orElse(null);
                String poisonedName = poisonedUser != null ? poisonedUser.getUsername() : "Unknown";
                createSystemMessage(game.getId(), poisonedName + " was poisoned during the night!");
            }
        }

        if (wolfVictimId == null && !healed) {
            createSystemMessage(game.getId(), "No one was killed during the night.");
        }
    }

    @Transactional
    public void resolveDayVoting(Game game) {
        List<GameAction> votes = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.DAY_VOTING, ActionType.VOTE_LYNCH);

        Long victimId = getMajorityTarget(votes);

        if (victimId != null) {
            GamePlayer victim = gamePlayerRepository.findById(victimId).orElse(null);
            if (victim != null) {
                killPlayer(victim, game);
                User victimUser = userRepository.findById(victim.getUserId()).orElse(null);
                String victimName = victimUser != null ? victimUser.getUsername() : "Unknown";
                createSystemMessage(game.getId(), victimName + " was lynched by the village!");
            }
        } else {
            createSystemMessage(game.getId(), "No one was lynched today.");
        }

        game.setDayNumber(game.getDayNumber() + 1);
        game.setCurrentPhase(GamePhase.NIGHT_WOLVES);
        gameRepository.save(game);
    }

    @Transactional
    public void killPlayer(GamePlayer player, Game game) {
        player.setIsAlive(false);
        player.setRevealedRole(true);
        gamePlayerRepository.save(player);

        RoleTemplate role = roleTemplateRepository.findById(player.getRoleId()).orElse(null);
        if (role != null && role.getName() == RoleName.HUNTER) {
            Map<String, Object> flags = fromJson(player.getStateFlagsJson());
            flags.put("hunterShotAvailable", true);
            player.setStateFlagsJson(toJson(flags));
            gamePlayerRepository.save(player);

            createSystemMessage(game.getId(), "The Hunter can now take revenge!");
        }
    }

    private Long getMajorityTarget(List<GameAction> votes) {
        if (votes.isEmpty()) return null;

        Map<Long, Long> voteCounts = votes.stream()
                .collect(Collectors.groupingBy(GameAction::getTargetPlayerId, Collectors.counting()));

        return voteCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Transactional
    public void checkWinCondition(Game game) {
        List<GamePlayer> alivePlayers = gamePlayerRepository.findByGameIdAndIsAlive(game.getId(), true);

        long aliveWerewolves = alivePlayers.stream()
                .filter(p -> {
                    RoleTemplate role = roleTemplateRepository.findById(p.getRoleId()).orElse(null);
                    return role != null && role.getFaction() == Faction.WOLVES;
                })
                .count();

        long aliveVillagers = alivePlayers.stream()
                .filter(p -> {
                    RoleTemplate role = roleTemplateRepository.findById(p.getRoleId()).orElse(null);
                    return role != null && role.getFaction() == Faction.VILLAGE;
                })
                .count();

        if (aliveWerewolves == 0) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinnerFaction(Faction.VILLAGE);
            game.setCurrentPhase(GamePhase.RESULT);
            game.setFinishedAt(Instant.now());
            gameRepository.save(game);
            createSystemMessage(game.getId(), "The Village wins! All werewolves have been eliminated!");
        } else if (aliveWerewolves >= aliveVillagers) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinnerFaction(Faction.WOLVES);
            game.setCurrentPhase(GamePhase.RESULT);
            game.setFinishedAt(Instant.now());
            gameRepository.save(game);
            createSystemMessage(game.getId(), "The Werewolves win! They have taken over the village!");
        }
    }

    @Transactional
    public void transitionToVoting(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        if (game.getCurrentPhase() != GamePhase.DAY_DISCUSSION) {
            throw new IllegalStateException("Can only transition to voting from discussion phase");
        }

        game.setCurrentPhase(GamePhase.DAY_VOTING);
        gameRepository.save(game);
        createSystemMessage(gameId, "Voting phase has begun! Vote for who to lynch.");
    }

    private List<String> calculateAvailableActions(Game game, GamePlayer player, RoleTemplate role) {
        List<String> actions = new ArrayList<>();

        if (!player.getIsAlive() && role.getName() != RoleName.HUNTER) {
            return actions;
        }

        Map<String, Object> flags = fromJson(player.getStateFlagsJson());

        switch (game.getCurrentPhase()) {
            case NIGHT_WOLVES:
                if (role.getName() == RoleName.WEREWOLF && player.getIsAlive()) {
                    actions.add("VOTE_WOLF_KILL");
                }
                break;

            case NIGHT_SEER:
                if (role.getName() == RoleName.SEER && player.getIsAlive()) {
                    actions.add("SEER_INSPECT");
                }
                break;

            case NIGHT_WITCH:
                if (role.getName() == RoleName.WITCH && player.getIsAlive()) {
                    if (Boolean.TRUE.equals(flags.get("healPotion"))) {
                        actions.add("WITCH_HEAL");
                    }
                    if (Boolean.TRUE.equals(flags.get("poisonPotion"))) {
                        actions.add("WITCH_POISON");
                    }
                }
                break;

            case DAY_VOTING:
                if (player.getIsAlive()) {
                    actions.add("VOTE_LYNCH");
                }
                break;

            case DAY_DISCUSSION:
                break;

            case RESULT:
                break;
        }

        if (Boolean.TRUE.equals(flags.get("hunterShotAvailable"))) {
            actions.add("HUNTER_SHOOT");
        }

        return actions;
    }

    private PlayerInfoDto toPlayerInfoDto(GamePlayer player, GamePlayer currentPlayer, RoleTemplate currentRole) {
        User user = userRepository.findById(player.getUserId()).orElse(null);
        RoleTemplate playerRole = roleTemplateRepository.findById(player.getRoleId()).orElse(null);

        PlayerInfoDto dto = new PlayerInfoDto();
        dto.setPlayerId(player.getId());
        dto.setUserId(player.getUserId());
        dto.setUsername(user != null ? user.getUsername() : "Unknown");
        dto.setAvatarConfig(user != null ? user.getAvatarConfig() : "default");
        dto.setSeatNumber(player.getSeatNumber());
        dto.setIsAlive(player.getIsAlive());
        dto.setRevealedRole(player.getRevealedRole());

        if (player.getId().equals(currentPlayer.getId()) || player.getRevealedRole()) {
            dto.setRole(playerRole != null ? playerRole.getName() : null);
        } else if (currentRole.getName() == RoleName.WEREWOLF && playerRole != null && playerRole.getName() == RoleName.WEREWOLF) {
            dto.setRole(RoleName.WEREWOLF);
        } else {
            dto.setRole(null);
        }

        return dto;
    }

    private String getPhaseDescription(GamePhase phase) {
        switch (phase) {
            case NIGHT_WOLVES: return "Night - Werewolves awaken";
            case NIGHT_SEER: return "Night - Seer investigates";
            case NIGHT_WITCH: return "Night - Witch decides";
            case DAY_DISCUSSION: return "Day - Discussion";
            case DAY_VOTING: return "Day - Voting";
            case RESULT: return "Game Over";
            default: return "Unknown phase";
        }
    }

    @Transactional
    public List<ChatMessageDto> getChatMessages(Long gameId, User currentUser, Long sinceTimestamp) {
        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not in this game"));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        RoleTemplate role = roleTemplateRepository.findById(player.getRoleId()).orElse(null);

        Instant since = sinceTimestamp != null ? Instant.ofEpochMilli(sinceTimestamp) : Instant.EPOCH;

        List<ChatChannel> allowedChannels = new ArrayList<>();
        allowedChannels.add(ChatChannel.SYSTEM);

        if (game.getCurrentPhase() == GamePhase.DAY_DISCUSSION || game.getCurrentPhase() == GamePhase.DAY_VOTING) {
            allowedChannels.add(ChatChannel.DAY);
        }

        if (role != null && role.getName() == RoleName.WEREWOLF) {
            allowedChannels.add(ChatChannel.NIGHT_WOLVES);
        }

        List<ChatMessage> messages = chatMessageRepository.findByGameIdAndChannelInAndCreatedAtAfterOrderByCreatedAt(
                gameId, allowedChannels, since);

        return messages.stream().map(this::toChatMessageDto).collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto sendChatMessage(Long gameId, User currentUser, ChatMessageRequest request) {
        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not in this game"));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        RoleTemplate role = roleTemplateRepository.findById(player.getRoleId()).orElse(null);

        ChatChannel channel;
        if (game.getCurrentPhase() == GamePhase.NIGHT_WOLVES && role != null && role.getName() == RoleName.WEREWOLF) {
            channel = ChatChannel.NIGHT_WOLVES;
        } else if (game.getCurrentPhase() == GamePhase.DAY_DISCUSSION || game.getCurrentPhase() == GamePhase.DAY_VOTING) {
            channel = ChatChannel.DAY;
        } else {
            throw new IllegalStateException("Cannot chat during this phase");
        }

        ChatMessage message = new ChatMessage();
        message.setGameId(gameId);
        message.setSenderUserId(currentUser.getId());
        message.setChannel(channel);
        message.setContent(request.getContent());
        message.setCreatedAt(Instant.now());

        message = chatMessageRepository.save(message);

        return toChatMessageDto(message);
    }

    private void createSystemMessage(Long gameId, String content) {
        ChatMessage message = new ChatMessage();
        message.setGameId(gameId);
        message.setSenderUserId(0L);
        message.setChannel(ChatChannel.SYSTEM);
        message.setContent(content);
        message.setCreatedAt(Instant.now());
        chatMessageRepository.save(message);
    }

    private ChatMessageDto toChatMessageDto(ChatMessage message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(message.getId());
        dto.setSenderUserId(message.getSenderUserId());

        if (message.getSenderUserId() > 0) {
            User sender = userRepository.findById(message.getSenderUserId()).orElse(null);
            dto.setSenderUsername(sender != null ? sender.getUsername() : "Unknown");
        } else {
            dto.setSenderUsername("System");
        }

        dto.setChannel(message.getChannel());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());

        return dto;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private WolfVictimDto getWolfVictimInternal(Game game) {
        List<GameAction> wolfVotes = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_WOLVES, ActionType.VOTE_WOLF_KILL);

        Long victimId = getMajorityTarget(wolfVotes);

        if (victimId == null) {
            return null;
        }

        GamePlayer victim = gamePlayerRepository.findById(victimId).orElse(null);
        if (victim == null) {
            return null;
        }

        User victimUser = userRepository.findById(victim.getUserId()).orElse(null);
        return new WolfVictimDto(victimId, victimUser != null ? victimUser.getUsername() : "Unknown");
    }

    private InspectionResultDto getLastInspectionResultInternal(Game game, GamePlayer seerPlayer) {
        List<GameAction> inspections = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_SEER, ActionType.SEER_INSPECT);

        GameAction lastInspection = inspections.stream()
                .filter(a -> a.getActorPlayerId().equals(seerPlayer.getId()))
                .findFirst()
                .orElse(null);

        if (lastInspection == null) {
            return null;
        }

        GamePlayer inspectedPlayer = gamePlayerRepository.findById(lastInspection.getTargetPlayerId()).orElse(null);
        if (inspectedPlayer == null) {
            return null;
        }

        RoleTemplate inspectedRole = roleTemplateRepository.findById(inspectedPlayer.getRoleId()).orElse(null);
        User inspectedUser = userRepository.findById(inspectedPlayer.getUserId()).orElse(null);

        return new InspectionResultDto(
                inspectedPlayer.getId(),
                inspectedUser != null ? inspectedUser.getUsername() : "Unknown",
                inspectedRole != null ? inspectedRole.getName() : null
        );
    }

    @Transactional(readOnly = true)
    public WolfVictimDto getWolfVictim(Long gameId, User currentUser) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not in this game"));

        RoleTemplate role = roleTemplateRepository.findById(player.getRoleId())
                .orElseThrow(() -> new IllegalStateException("Role not found"));

        if (role.getName() != RoleName.WITCH) {
            throw new IllegalStateException("Only the Witch can see the wolf victim");
        }

        if (game.getCurrentPhase() != GamePhase.NIGHT_WITCH) {
            throw new IllegalStateException("Can only see victim during Witch phase");
        }

        List<GameAction> wolfVotes = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_WOLVES, ActionType.VOTE_WOLF_KILL);

        Long victimId = getMajorityTarget(wolfVotes);

        if (victimId == null) {
            return null;
        }

        GamePlayer victim = gamePlayerRepository.findById(victimId).orElse(null);
        if (victim == null) {
            return null;
        }

        User victimUser = userRepository.findById(victim.getUserId()).orElse(null);
        return new WolfVictimDto(victimId, victimUser != null ? victimUser.getUsername() : "Unknown");
    }

    @Transactional(readOnly = true)
    public InspectionResultDto getLastInspectionResult(Long gameId, User currentUser) {
        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not in this game"));

        RoleTemplate role = roleTemplateRepository.findById(player.getRoleId())
                .orElseThrow(() -> new IllegalStateException("Role not found"));

        if (role.getName() != RoleName.SEER) {
            throw new IllegalStateException("Only the Seer can see inspection results");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        List<GameAction> inspections = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_SEER, ActionType.SEER_INSPECT);

        if (inspections.isEmpty()) {
            return null;
        }

        GameAction lastInspection = inspections.get(inspections.size() - 1);
        if (!lastInspection.getActorPlayerId().equals(player.getId())) {
            return null;
        }

        GamePlayer inspectedPlayer = gamePlayerRepository.findById(lastInspection.getTargetPlayerId()).orElse(null);
        if (inspectedPlayer == null) {
            return null;
        }

        RoleTemplate inspectedRole = roleTemplateRepository.findById(inspectedPlayer.getRoleId()).orElse(null);
        User inspectedUser = userRepository.findById(inspectedPlayer.getUserId()).orElse(null);

        return new InspectionResultDto(
                inspectedPlayer.getId(),
                inspectedUser != null ? inspectedUser.getUsername() : "Unknown",
                inspectedRole != null ? inspectedRole.getName() : null
        );
    }
}


