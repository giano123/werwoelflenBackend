package com.ausganslage.ausgangslageBackend.service;

import com.ausganslage.ausgangslageBackend.dto.*;
import com.ausganslage.ausgangslageBackend.enums.*;
import com.ausganslage.ausgangslageBackend.exception.InvalidActionException;
import com.ausganslage.ausgangslageBackend.exception.InvalidGameStateException;
import com.ausganslage.ausgangslageBackend.exception.ResourceNotFoundException;
import com.ausganslage.ausgangslageBackend.exception.UnauthorizedActionException;
import com.ausganslage.ausgangslageBackend.model.*;
import com.ausganslage.ausgangslageBackend.repository.*;
import com.ausganslage.ausgangslageBackend.util.AuditLogger;
import com.ausganslage.ausgangslageBackend.util.LoggingContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

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
        logger.info("Starting game: lobbyCode={}, requestedBy userId={}, username={}",
            lobbyCode, currentUser.getId(), currentUser.getUsername());
        LoggingContext.setAction("START_GAME");
        LoggingContext.setUserId(currentUser.getId());
        LoggingContext.setUsername(currentUser.getUsername());

        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode)
                .orElseThrow(() -> {
                    logger.warn("Start game failed - lobby not found: lobbyCode={}", lobbyCode);
                    return new ResourceNotFoundException("Lobby", lobbyCode);
                });

        LoggingContext.setLobbyId(lobby.getId());

        if (!lobby.getHostUserId().equals(currentUser.getId())) {
            logger.warn("Start game failed - not host: lobbyId={}, userId={}, hostUserId={}",
                lobby.getId(), currentUser.getId(), lobby.getHostUserId());
            AuditLogger.logUnauthorizedAccess(currentUser.getId(), currentUser.getUsername(),
                "Start game (not host)");
            throw new UnauthorizedActionException("Only the host can start the game", currentUser.getId(), "START_GAME");
        }

        if (lobby.getStatus() != LobbyStatus.OPEN) {
            logger.warn("Start game failed - lobby not open: lobbyId={}, status={}",
                lobby.getId(), lobby.getStatus());
            throw new InvalidGameStateException("Lobby is not open", lobby.getStatus().toString(), LobbyStatus.OPEN.toString());
        }

        List<LobbyMember> members = lobbyMemberRepository.findByLobbyId(lobby.getId());

        logger.debug("Checking game start conditions: lobbyId={}, playerCount={}",
            lobby.getId(), members.size());

        if (members.size() < 4) {
            logger.warn("Start game failed - not enough players: lobbyId={}, playerCount={}",
                lobby.getId(), members.size());
            throw new InvalidActionException("START_GAME", "Need at least 4 players to start");
        }

        long notReady = members.stream().filter(m -> !m.getIsReady()).count();
        if (notReady > 0) {
            logger.warn("Start game failed - players not ready: lobbyId={}, notReadyCount={}",
                lobby.getId(), notReady);
            throw new InvalidActionException("START_GAME", "All players must be ready");
        }

        logger.info("Creating game: lobbyId={}, playerCount={}", lobby.getId(), members.size());

        Game game = new Game();
        game.setLobbyId(lobby.getId());
        game.setStatus(GameStatus.RUNNING);
        game.setCurrentPhase(GamePhase.NIGHT_WOLVES);
        game.setDayNumber(1);
        game.setCreatedAt(Instant.now());

        game = gameRepository.save(game);

        LoggingContext.setGameId(game.getId());
        logger.info("Game entity created: gameId={}, lobbyId={}", game.getId(), lobby.getId());

        distributeRoles(game, members);

        lobby.setStatus(LobbyStatus.IN_GAME);
        lobbyRepository.save(lobby);

        AuditLogger.logGameStarted(game.getId(), lobby.getId(), members.size(), currentUser.getId());
        logger.info("Game started successfully: gameId={}, lobbyId={}, playerCount={}",
            game.getId(), lobby.getId(), members.size());

        createSystemMessage(game.getId(), "Game started! Night falls...");

        return game;
    }

    private void distributeRoles(Game game, List<LobbyMember> members) {
        logger.info("Distributing roles: gameId={}, playerCount={}", game.getId(), members.size());

        int playerCount = members.size();
        List<RoleTemplate> rolesToAssign = new ArrayList<>();

        int werewolfCount = Math.max(1, playerCount / 4);
        logger.debug("Calculating werewolf count: gameId={}, playerCount={}, werewolfCount={}",
            game.getId(), playerCount, werewolfCount);

        RoleTemplate werewolf = roleTemplateRepository.findByName(RoleName.WEREWOLF)
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", "WEREWOLF"));
        for (int i = 0; i < werewolfCount; i++) {
            rolesToAssign.add(werewolf);
        }

        RoleTemplate seer = roleTemplateRepository.findByName(RoleName.SEER)
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", "SEER"));
        rolesToAssign.add(seer);

        RoleTemplate witch = roleTemplateRepository.findByName(RoleName.WITCH)
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", "WITCH"));
        rolesToAssign.add(witch);

        RoleTemplate hunter = roleTemplateRepository.findByName(RoleName.HUNTER)
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", "HUNTER"));
        rolesToAssign.add(hunter);

        RoleTemplate villager = roleTemplateRepository.findByName(RoleName.VILLAGER)
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", "VILLAGER"));
        while (rolesToAssign.size() < playerCount) {
            rolesToAssign.add(villager);
        }

        logger.debug("Role distribution composition: gameId={}, werewolves={}, villagers={}, special roles=3",
            game.getId(), werewolfCount, playerCount - werewolfCount - 3);

        Collections.shuffle(rolesToAssign);

        List<LobbyMember> shuffledMembers = new ArrayList<>(members);
        Collections.shuffle(shuffledMembers);

        logger.debug("Assigning roles to players: gameId={}", game.getId());

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

            User user = userRepository.findById(member.getUserId()).orElse(null);
            String username = user != null ? user.getUsername() : "Unknown";

            logger.debug("Role assigned: gameId={}, userId={}, username={}, role={}, seatNumber={}",
                game.getId(), member.getUserId(), username, role.getName(), i + 1);
            AuditLogger.logRoleAssignment(game.getId(), player.getId(), username, role.getName().toString());
        }

        logger.info("All roles distributed successfully: gameId={}, playerCount={}",
            game.getId(), shuffledMembers.size());
    }

    @Transactional(readOnly = true)
    public GameStateDto getGameState(Long gameId, User currentUser) {
        LoggingContext.setGameId(gameId);
        LoggingContext.setUserId(currentUser.getId());
        LoggingContext.setUsername(currentUser.getUsername());

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> {
                    logger.warn("Get game state failed - game not found: gameId={}", gameId);
                    return new ResourceNotFoundException("Game", gameId);
                });

        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> {
                    logger.warn("Get game state failed - user not in game: gameId={}, userId={}",
                        gameId, currentUser.getId());
                    return new UnauthorizedActionException("You are not in this game", currentUser.getId(), "GET_GAME_STATE");
                });

        RoleTemplate currentRole = roleTemplateRepository.findById(currentPlayer.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", currentPlayer.getRoleId()));

        logger.trace("Game state retrieved: gameId={}, userId={}, phase={}, dayNumber={}, isAlive={}",
            gameId, currentUser.getId(), game.getCurrentPhase(), game.getDayNumber(), currentPlayer.getIsAlive());

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
        logger.info("Vote submitted: gameId={}, userId={}, username={}, targetPlayerId={}",
            gameId, currentUser.getId(), currentUser.getUsername(), request.getTargetPlayerId());
        LoggingContext.setAction("SUBMIT_VOTE");
        LoggingContext.setGameId(gameId);
        LoggingContext.setUserId(currentUser.getId());
        LoggingContext.setUsername(currentUser.getUsername());

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> {
                    logger.warn("Vote failed - game not found: gameId={}", gameId);
                    return new ResourceNotFoundException("Game", gameId);
                });

        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> {
                    logger.warn("Vote failed - user not in game: gameId={}, userId={}",
                        gameId, currentUser.getId());
                    return new UnauthorizedActionException("You are not in this game", currentUser.getId(), "SUBMIT_VOTE");
                });

        if (!currentPlayer.getIsAlive()) {
            logger.warn("Vote failed - player is dead: gameId={}, playerId={}",
                gameId, currentPlayer.getId());
            throw new InvalidActionException("SUBMIT_VOTE", "Dead players cannot vote");
        }

        RoleTemplate currentRole = roleTemplateRepository.findById(currentPlayer.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", currentPlayer.getRoleId()));

        ActionType actionType;
        if (game.getCurrentPhase() == GamePhase.DAY_VOTING) {
            actionType = ActionType.VOTE_LYNCH;
            logger.debug("Lynch vote: gameId={}, voterId={}, phase={}",
                gameId, currentPlayer.getId(), game.getCurrentPhase());
        } else if (game.getCurrentPhase() == GamePhase.NIGHT_WOLVES && currentRole.getName() == RoleName.WEREWOLF) {
            actionType = ActionType.VOTE_WOLF_KILL;
            logger.debug("Wolf kill vote: gameId={}, wolvesVoterId={}, phase={}",
                gameId, currentPlayer.getId(), game.getCurrentPhase());
        } else {
            logger.warn("Vote failed - invalid phase: gameId={}, phase={}, role={}",
                gameId, game.getCurrentPhase(), currentRole.getName());
            throw new InvalidGameStateException("Invalid voting phase", game.getCurrentPhase().toString(), "DAY_VOTING or NIGHT_WOLVES");
        }

        GamePlayer targetPlayer = gamePlayerRepository.findById(request.getTargetPlayerId())
                .orElseThrow(() -> {
                    logger.warn("Vote failed - target not found: gameId={}, targetPlayerId={}",
                        gameId, request.getTargetPlayerId());
                    return new ResourceNotFoundException("GamePlayer", request.getTargetPlayerId());
                });

        if (!targetPlayer.getIsAlive()) {
            logger.warn("Vote failed - target is dead: gameId={}, targetPlayerId={}",
                gameId, request.getTargetPlayerId());
            throw new InvalidActionException("SUBMIT_VOTE", "Cannot vote for dead player");
        }

        if (actionType == ActionType.VOTE_WOLF_KILL && currentRole.getName() == RoleName.WEREWOLF) {
            RoleTemplate targetRole = roleTemplateRepository.findById(targetPlayer.getRoleId()).orElse(null);
            if (targetRole != null && targetRole.getName() == RoleName.WEREWOLF) {
                logger.warn("Vote failed - werewolf trying to kill werewolf: gameId={}, voterId={}, targetId={}",
                    gameId, currentPlayer.getId(), targetPlayer.getId());
                throw new InvalidActionException("VOTE_WOLF_KILL", "Werewolves cannot kill each other");
            }
        }

        Optional<GameAction> existingVote = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActorPlayerId(
                gameId, game.getDayNumber(), game.getCurrentPhase(), currentPlayer.getId());

        if (existingVote.isPresent()) {
            logger.debug("Removing existing vote: gameId={}, playerId={}, oldTargetId={}",
                gameId, currentPlayer.getId(), existingVote.get().getTargetPlayerId());
            gameActionRepository.delete(existingVote.get());
        }

        GameAction action = new GameAction();
        action.setGameId(gameId);
        action.setDayNumber(game.getDayNumber());
        action.setPhase(game.getCurrentPhase());
        action.setActorPlayerId(currentPlayer.getId());
        action.setTargetPlayerId(targetPlayer.getId());
        action.setActionType(actionType);
        action.setCreatedAt(Instant.now());

        gameActionRepository.save(action);

        User targetUser = userRepository.findById(targetPlayer.getUserId()).orElse(null);
        String targetUsername = targetUser != null ? targetUser.getUsername() : "Unknown";

        AuditLogger.logPlayerAction(gameId, currentPlayer.getId(), currentUser.getUsername(),
            actionType.toString(), targetPlayer.getId(), targetUsername, game.getCurrentPhase().toString());
        logger.info("Vote recorded: gameId={}, actionType={}, voter={}, target={}, phase={}",
            gameId, actionType, currentUser.getUsername(), targetUsername, game.getCurrentPhase());

        checkAndAdvancePhase(game);
    }

    @Transactional
    public void submitPowerAction(Long gameId, User currentUser, PowerActionRequest request) {
        logger.info("Power action submitted: gameId={}, userId={}, username={}, actionType={}, targetPlayerId={}",
            gameId, currentUser.getId(), currentUser.getUsername(), request.getActionType(), request.getTargetPlayerId());
        LoggingContext.setAction("POWER_ACTION");
        LoggingContext.setGameId(gameId);
        LoggingContext.setUserId(currentUser.getId());
        LoggingContext.setUsername(currentUser.getUsername());

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> {
                    logger.warn("Power action failed - game not found: gameId={}", gameId);
                    return new ResourceNotFoundException("Game", gameId);
                });

        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> {
                    logger.warn("Power action failed - user not in game: gameId={}, userId={}",
                        gameId, currentUser.getId());
                    return new UnauthorizedActionException("You are not in this game", currentUser.getId(), "POWER_ACTION");
                });

        if (!currentPlayer.getIsAlive() && request.getActionType() != ActionType.HUNTER_SHOOT) {
            logger.warn("Power action failed - player is dead: gameId={}, playerId={}, actionType={}",
                gameId, currentPlayer.getId(), request.getActionType());
            throw new InvalidActionException("POWER_ACTION", "Dead players cannot use powers");
        }

        RoleTemplate currentRole = roleTemplateRepository.findById(currentPlayer.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", currentPlayer.getRoleId()));

        logger.debug("Validating power action: gameId={}, playerId={}, role={}, actionType={}",
            gameId, currentPlayer.getId(), currentRole.getName(), request.getActionType());

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

        String targetUsername = "None";
        if (request.getTargetPlayerId() != null) {
            GamePlayer targetPlayer = gamePlayerRepository.findById(request.getTargetPlayerId()).orElse(null);
            if (targetPlayer != null) {
                User targetUser = userRepository.findById(targetPlayer.getUserId()).orElse(null);
                targetUsername = targetUser != null ? targetUser.getUsername() : "Unknown";
            }
        }

        AuditLogger.logPlayerAction(gameId, currentPlayer.getId(), currentUser.getUsername(),
            request.getActionType().toString(), request.getTargetPlayerId(), targetUsername,
            game.getCurrentPhase().toString());
        logger.info("Power action recorded: gameId={}, actionType={}, actor={}, target={}",
            gameId, request.getActionType(), currentUser.getUsername(), targetUsername);

        if (request.getActionType() == ActionType.WITCH_HEAL || request.getActionType() == ActionType.WITCH_POISON) {
            logger.debug("Updating witch potions: gameId={}, playerId={}, actionType={}",
                gameId, currentPlayer.getId(), request.getActionType());
            updateWitchPotions(currentPlayer, request.getActionType());
        }

        if (request.getActionType() == ActionType.HUNTER_SHOOT) {
            logger.info("Hunter shooting: gameId={}, hunterId={}, targetId={}",
                gameId, currentPlayer.getId(), request.getTargetPlayerId());

            Map<String, Object> flags = fromJson(currentPlayer.getStateFlagsJson());
            flags.put("hunterShotAvailable", false);
            currentPlayer.setStateFlagsJson(toJson(flags));
            gamePlayerRepository.save(currentPlayer);

            GamePlayer target = gamePlayerRepository.findById(request.getTargetPlayerId())
                    .orElseThrow(() -> new ResourceNotFoundException("GamePlayer", request.getTargetPlayerId()));
            killPlayer(target, game);
        }

        checkAndAdvancePhase(game);
    }

    @Transactional
    public void skipAction(Long gameId, User currentUser) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", gameId));

        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("You are not in this game", currentUser.getId(), "SKIP_ACTION"));

        if (!currentPlayer.getIsAlive()) {
            throw new InvalidActionException("SKIP_ACTION", "Dead players cannot skip");
        }

        RoleTemplate currentRole = roleTemplateRepository.findById(currentPlayer.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", currentPlayer.getRoleId()));

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
            throw new InvalidGameStateException("Cannot skip during this phase", game.getCurrentPhase().toString(), "NIGHT_SEER or NIGHT_WITCH");
        }

        checkAndAdvancePhase(game);
    }

    private void validatePowerAction(Game game, GamePlayer player, RoleTemplate role, PowerActionRequest request) {
        Map<String, Object> flags = fromJson(player.getStateFlagsJson());

        switch (request.getActionType()) {
            case SEER_INSPECT:
                if (role.getName() != RoleName.SEER) {
                    throw new UnauthorizedActionException("Only the Seer can inspect", player.getUserId(), "SEER_INSPECT");
                }
                if (game.getCurrentPhase() != GamePhase.NIGHT_SEER) {
                    throw new InvalidGameStateException("Can only inspect during Seer phase", game.getCurrentPhase().toString(), GamePhase.NIGHT_SEER.toString());
                }
                break;

            case WITCH_HEAL:
                if (role.getName() != RoleName.WITCH) {
                    throw new UnauthorizedActionException("Only the Witch can heal", player.getUserId(), "WITCH_HEAL");
                }
                if (game.getCurrentPhase() != GamePhase.NIGHT_WITCH) {
                    throw new InvalidGameStateException("Can only heal during Witch phase", game.getCurrentPhase().toString(), GamePhase.NIGHT_WITCH.toString());
                }
                if (!Boolean.TRUE.equals(flags.get("healPotion"))) {
                    throw new InvalidActionException("WITCH_HEAL", "Heal potion already used");
                }
                Long wolfVictimId = getWolfVictimId(game);
                if (wolfVictimId == null) {
                    throw new InvalidActionException("WITCH_HEAL", "No wolf victim to heal");
                }
                if (request.getTargetPlayerId() != null && !request.getTargetPlayerId().equals(wolfVictimId)) {
                    throw new InvalidActionException("WITCH_HEAL", "Can only heal the wolf victim");
                }
                break;

            case WITCH_POISON:
                if (role.getName() != RoleName.WITCH) {
                    throw new UnauthorizedActionException("Only the Witch can poison", player.getUserId(), "WITCH_POISON");
                }
                if (game.getCurrentPhase() != GamePhase.NIGHT_WITCH) {
                    throw new InvalidGameStateException("Can only poison during Witch phase", game.getCurrentPhase().toString(), GamePhase.NIGHT_WITCH.toString());
                }
                if (!Boolean.TRUE.equals(flags.get("poisonPotion"))) {
                    throw new InvalidActionException("WITCH_POISON", "Poison potion already used");
                }
                break;

            case HUNTER_SHOOT:
                if (role.getName() != RoleName.HUNTER) {
                    throw new UnauthorizedActionException("Only the Hunter can shoot", player.getUserId(), "HUNTER_SHOOT");
                }
                if (!Boolean.TRUE.equals(flags.get("hunterShotAvailable"))) {
                    throw new InvalidActionException("HUNTER_SHOOT", "Hunter shot not available");
                }
                if (game.getCurrentPhase() != GamePhase.DAY_DISCUSSION &&
                    game.getCurrentPhase() != GamePhase.DAY_VOTING &&
                    game.getCurrentPhase() != GamePhase.NIGHT_WITCH) {
                    throw new InvalidGameStateException("Hunter can only shoot during day phases or after night resolution",
                        game.getCurrentPhase().toString(), "DAY_DISCUSSION, DAY_VOTING, or NIGHT_WITCH");
                }
                break;

            default:
                throw new InvalidActionException("POWER_ACTION", "Invalid power action type");
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
        logger.debug("Checking phase advancement: gameId={}, currentPhase={}, dayNumber={}",
            game.getId(), game.getCurrentPhase(), game.getDayNumber());

        List<GamePlayer> alivePlayers = gamePlayerRepository.findByGameIdAndIsAlive(game.getId(), true);

        logger.trace("Alive players count: gameId={}, count={}", game.getId(), alivePlayers.size());

        GamePhase oldPhase = game.getCurrentPhase();

        switch (game.getCurrentPhase()) {
            case NIGHT_WOLVES:
                if (hasAllWerewolvesVoted(game, alivePlayers)) {
                    logger.info("All werewolves voted - advancing to Seer phase: gameId={}", game.getId());
                    game.setCurrentPhase(GamePhase.NIGHT_SEER);
                    gameRepository.save(game);
                    AuditLogger.logPhaseChange(game.getId(), oldPhase.toString(),
                        GamePhase.NIGHT_SEER.toString(), game.getDayNumber());
                } else {
                    logger.trace("Waiting for werewolf votes: gameId={}", game.getId());
                }
                break;

            case NIGHT_SEER:
                if (hasSeerActed(game, alivePlayers) || !hasSeerAlive(alivePlayers)) {
                    logger.info("Seer phase complete - advancing to Witch phase: gameId={}, seerActed={}, seerAlive={}",
                        game.getId(), hasSeerActed(game, alivePlayers), hasSeerAlive(alivePlayers));
                    game.setCurrentPhase(GamePhase.NIGHT_WITCH);
                    gameRepository.save(game);
                    AuditLogger.logPhaseChange(game.getId(), oldPhase.toString(),
                        GamePhase.NIGHT_WITCH.toString(), game.getDayNumber());
                } else {
                    logger.trace("Waiting for seer action: gameId={}", game.getId());
                }
                break;

            case NIGHT_WITCH:
                if (hasWitchActed(game, alivePlayers) || !hasWitchAlive(alivePlayers)) {
                    logger.info("Witch phase complete - resolving night actions: gameId={}, witchActed={}, witchAlive={}",
                        game.getId(), hasWitchActed(game, alivePlayers), hasWitchAlive(alivePlayers));
                    resolveNightActions(game);
                    game.setCurrentPhase(GamePhase.DAY_DISCUSSION);
                    gameRepository.save(game);
                    AuditLogger.logPhaseChange(game.getId(), oldPhase.toString(),
                        GamePhase.DAY_DISCUSSION.toString(), game.getDayNumber());
                } else {
                    logger.trace("Waiting for witch action: gameId={}", game.getId());
                }
                break;

            case DAY_DISCUSSION:
                logger.trace("Day discussion phase - no automatic advancement: gameId={}", game.getId());
                break;

            case DAY_VOTING:
                if (hasAllAlivePlayersVoted(game, alivePlayers)) {
                    logger.info("All players voted - resolving day voting: gameId={}", game.getId());
                    resolveDayVoting(game);
                } else {
                    logger.trace("Waiting for all players to vote: gameId={}", game.getId());
                }
                break;

            case RESULT:
                logger.trace("Game in result phase: gameId={}", game.getId());
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

        Set<Long> votedWerewolfIds = votes.stream()
                .map(GameAction::getActorPlayerId)
                .collect(Collectors.toSet());

        Set<Long> aliveWerewolfIds = aliveWerewolves.stream()
                .map(GamePlayer::getId)
                .collect(Collectors.toSet());

        return votedWerewolfIds.containsAll(aliveWerewolfIds);
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

        Set<Long> votedPlayerIds = votes.stream()
                .map(GameAction::getActorPlayerId)
                .collect(Collectors.toSet());

        Set<Long> alivePlayerIds = alivePlayers.stream()
                .map(GamePlayer::getId)
                .collect(Collectors.toSet());

        return votedPlayerIds.containsAll(alivePlayerIds);
    }

    @Transactional
    public void resolveNightActions(Game game) {
        logger.info("Resolving night actions: gameId={}, dayNumber={}", game.getId(), game.getDayNumber());

        List<GameAction> wolfVotes = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_WOLVES, ActionType.VOTE_WOLF_KILL);

        logger.debug("Wolf votes count: gameId={}, voteCount={}", game.getId(), wolfVotes.size());

        Long wolfVictimId = getMajorityTarget(wolfVotes);

        if (wolfVictimId != null) {
            logger.info("Werewolves chose victim: gameId={}, victimPlayerId={}", game.getId(), wolfVictimId);
        } else {
            logger.info("No wolf victim (tie or no votes): gameId={}", game.getId());
        }

        List<GameAction> witchActions = gameActionRepository.findByGameIdAndDayNumberAndPhase(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_WITCH);

        logger.debug("Witch actions count: gameId={}, actionCount={}", game.getId(), witchActions.size());

        boolean healed = false;
        Long poisonedPlayerId = null;

        for (GameAction action : witchActions) {
            if (action.getActionType() == ActionType.WITCH_HEAL) {
                if (action.getTargetPlayerId().equals(wolfVictimId)) {
                    healed = true;
                    logger.info("Witch healed wolf victim: gameId={}, victimPlayerId={}",
                        game.getId(), wolfVictimId);
                    createSystemMessage(game.getId(), "The Witch saved someone from the wolves!");
                }
            } else if (action.getActionType() == ActionType.WITCH_POISON) {
                poisonedPlayerId = action.getTargetPlayerId();
                logger.info("Witch poisoned player: gameId={}, poisonedPlayerId={}",
                    game.getId(), poisonedPlayerId);
            }
        }

        if (wolfVictimId != null && !healed) {
            GamePlayer victim = gamePlayerRepository.findById(wolfVictimId).orElse(null);
            if (victim != null) {
                User victimUser = userRepository.findById(victim.getUserId()).orElse(null);
                String victimName = victimUser != null ? victimUser.getUsername() : "Unknown";

                logger.info("Wolf victim dies: gameId={}, playerId={}, username={}",
                    game.getId(), victim.getId(), victimName);
                AuditLogger.logPlayerDeath(game.getId(), victim.getId(), victimName,
                    "WOLF_KILL", game.getDayNumber());

                killPlayer(victim, game);
                createSystemMessage(game.getId(), victimName + " was killed by werewolves during the night!");
            }
        }

        if (poisonedPlayerId != null) {
            GamePlayer poisoned = gamePlayerRepository.findById(poisonedPlayerId).orElse(null);
            if (poisoned != null && poisoned.getIsAlive()) {
                User poisonedUser = userRepository.findById(poisoned.getUserId()).orElse(null);
                String poisonedName = poisonedUser != null ? poisonedUser.getUsername() : "Unknown";

                logger.info("Poisoned player dies: gameId={}, playerId={}, username={}",
                    game.getId(), poisoned.getId(), poisonedName);
                AuditLogger.logPlayerDeath(game.getId(), poisoned.getId(), poisonedName,
                    "WITCH_POISON", game.getDayNumber());

                killPlayer(poisoned, game);
                createSystemMessage(game.getId(), poisonedName + " was poisoned during the night!");
            }
        }

        if (wolfVictimId == null && !healed) {
            logger.info("No deaths during night: gameId={}", game.getId());
            createSystemMessage(game.getId(), "No one was killed during the night.");
        }
    }

    @Transactional
    public void resolveDayVoting(Game game) {
        logger.info("Resolving day voting: gameId={}, dayNumber={}", game.getId(), game.getDayNumber());

        List<GameAction> votes = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.DAY_VOTING, ActionType.VOTE_LYNCH);

        logger.debug("Lynch votes count: gameId={}, voteCount={}", game.getId(), votes.size());

        Long victimId = getMajorityTarget(votes);

        if (victimId != null) {
            logger.info("Village voted to lynch player: gameId={}, victimPlayerId={}", game.getId(), victimId);

            GamePlayer victim = gamePlayerRepository.findById(victimId).orElse(null);
            if (victim != null) {
                User victimUser = userRepository.findById(victim.getUserId()).orElse(null);
                String victimName = victimUser != null ? victimUser.getUsername() : "Unknown";

                long voteCount = votes.stream().filter(v -> v.getTargetPlayerId().equals(victimId)).count();
                logger.info("Player lynched by village: gameId={}, playerId={}, username={}, votesReceived={}",
                    game.getId(), victim.getId(), victimName, voteCount);
                AuditLogger.logPlayerDeath(game.getId(), victim.getId(), victimName,
                    "LYNCH", game.getDayNumber());
                AuditLogger.logVoteResult(game.getId(), victim.getId(), victimName,
                    (int) voteCount, votes.size(), GamePhase.DAY_VOTING.toString());

                killPlayer(victim, game);
                createSystemMessage(game.getId(), victimName + " was lynched by the village!");
            }
        } else {
            logger.info("No lynch (tie or no votes): gameId={}", game.getId());
            createSystemMessage(game.getId(), "No one was lynched today.");
        }

        logger.info("Advancing to next night: gameId={}, nextDayNumber={}",
            game.getId(), game.getDayNumber() + 1);
        game.setDayNumber(game.getDayNumber() + 1);
        game.setCurrentPhase(GamePhase.NIGHT_WOLVES);
        gameRepository.save(game);
        AuditLogger.logPhaseChange(game.getId(), GamePhase.DAY_VOTING.toString(),
            GamePhase.NIGHT_WOLVES.toString(), game.getDayNumber());
    }

    @Transactional
    public void killPlayer(GamePlayer player, Game game) {
        logger.debug("Killing player: gameId={}, playerId={}", game.getId(), player.getId());

        player.setIsAlive(false);
        player.setRevealedRole(true);
        gamePlayerRepository.save(player);

        RoleTemplate role = roleTemplateRepository.findById(player.getRoleId()).orElse(null);
        if (role != null && role.getName() == RoleName.HUNTER) {
            logger.info("Hunter killed - shot becomes available: gameId={}, hunterId={}",
                game.getId(), player.getId());
            Map<String, Object> flags = fromJson(player.getStateFlagsJson());
            flags.put("hunterShotAvailable", true);
            player.setStateFlagsJson(toJson(flags));
            gamePlayerRepository.save(player);

            User hunterUser = userRepository.findById(player.getUserId()).orElse(null);
            String hunterName = hunterUser != null ? hunterUser.getUsername() : "The Hunter";
            createSystemMessage(game.getId(), hunterName + " was a Hunter! They can now take revenge!");
        }
    }

    private Long getMajorityTarget(List<GameAction> votes) {
        if (votes.isEmpty()) return null;

        Map<Long, Long> voteCounts = votes.stream()
                .collect(Collectors.groupingBy(GameAction::getTargetPlayerId, Collectors.counting()));

        long maxVotes = voteCounts.values().stream()
                .max(Long::compare)
                .orElse(0L);

        List<Long> topVoted = voteCounts.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topVoted.size() > 1) {
            return null;
        }

        return topVoted.get(0);
    }

    @Transactional
    public void checkWinCondition(Game game) {
        logger.trace("Checking win condition: gameId={}", game.getId());

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

        logger.debug("Win condition check: gameId={}, aliveWerewolves={}, aliveVillagers={}, totalAlive={}",
            game.getId(), aliveWerewolves, aliveVillagers, alivePlayers.size());

        if (aliveWerewolves == 0) {
            long gameDuration = game.getCreatedAt() != null ?
                Instant.now().getEpochSecond() - game.getCreatedAt().getEpochSecond() : 0;

            logger.info("GAME ENDED - Village wins: gameId={}, dayNumber={}, durationSeconds={}",
                game.getId(), game.getDayNumber(), gameDuration);
            AuditLogger.logGameEnded(game.getId(), Faction.VILLAGE.toString(), (int) gameDuration);

            game.setStatus(GameStatus.FINISHED);
            game.setWinnerFaction(Faction.VILLAGE);
            game.setCurrentPhase(GamePhase.RESULT);
            game.setFinishedAt(Instant.now());
            gameRepository.save(game);
            createSystemMessage(game.getId(), "The Village wins! All werewolves have been eliminated!");
        } else if (aliveWerewolves >= aliveVillagers) {
            long gameDuration = game.getCreatedAt() != null ?
                Instant.now().getEpochSecond() - game.getCreatedAt().getEpochSecond() : 0;

            logger.info("GAME ENDED - Werewolves win: gameId={}, dayNumber={}, durationSeconds={}, werewolves={}, villagers={}",
                game.getId(), game.getDayNumber(), gameDuration, aliveWerewolves, aliveVillagers);
            AuditLogger.logGameEnded(game.getId(), Faction.WOLVES.toString(), (int) gameDuration);

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
        logger.info("Transitioning to voting phase: gameId={}", gameId);
        LoggingContext.setGameId(gameId);
        LoggingContext.setAction("TRANSITION_TO_VOTING");

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> {
                    logger.warn("Transition to voting failed - game not found: gameId={}", gameId);
                    return new ResourceNotFoundException("Game", gameId);
                });

        if (game.getCurrentPhase() != GamePhase.DAY_DISCUSSION) {
            logger.warn("Transition to voting failed - wrong phase: gameId={}, currentPhase={}",
                gameId, game.getCurrentPhase());
            throw new InvalidGameStateException("Can only transition to voting from discussion phase",
                game.getCurrentPhase().toString(), GamePhase.DAY_DISCUSSION.toString());
        }

        game.setCurrentPhase(GamePhase.DAY_VOTING);
        gameRepository.save(game);

        AuditLogger.logPhaseChange(gameId, GamePhase.DAY_DISCUSSION.toString(),
            GamePhase.DAY_VOTING.toString(), game.getDayNumber());
        logger.info("Transitioned to voting phase: gameId={}, dayNumber={}", gameId, game.getDayNumber());

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
                .orElseThrow(() -> new UnauthorizedActionException("You are not in this game", currentUser.getId(), "GET_CHAT_MESSAGES"));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", gameId));

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
    public void sendChatMessage(Long gameId, User currentUser, ChatMessageRequest request) {
        logger.debug("Chat message sent: gameId={}, userId={}, username={}, messageLength={}",
            gameId, currentUser.getId(), currentUser.getUsername(),
            request.getContent() != null ? request.getContent().length() : 0);
        LoggingContext.setGameId(gameId);
        LoggingContext.setUserId(currentUser.getId());
        LoggingContext.setUsername(currentUser.getUsername());
        LoggingContext.setAction("SEND_CHAT");

        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> {
                    logger.warn("Send chat failed - user not in game: gameId={}, userId={}",
                        gameId, currentUser.getId());
                    return new UnauthorizedActionException("You are not in this game", currentUser.getId(), "SEND_CHAT");
                });

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", gameId));

        RoleTemplate role = roleTemplateRepository.findById(player.getRoleId()).orElse(null);

        ChatChannel channel;
        if (game.getCurrentPhase() == GamePhase.NIGHT_WOLVES && role != null && role.getName() == RoleName.WEREWOLF) {
            channel = ChatChannel.NIGHT_WOLVES;
            logger.trace("Chat to wolves channel: gameId={}, userId={}", gameId, currentUser.getId());
        } else if (game.getCurrentPhase() == GamePhase.DAY_DISCUSSION || game.getCurrentPhase() == GamePhase.DAY_VOTING) {
            channel = ChatChannel.DAY;
            logger.trace("Chat to day channel: gameId={}, userId={}", gameId, currentUser.getId());
        } else {
            logger.warn("Send chat failed - invalid phase: gameId={}, phase={}, role={}",
                gameId, game.getCurrentPhase(), role != null ? role.getName() : "null");
            throw new InvalidGameStateException("Cannot chat during this phase",
                game.getCurrentPhase().toString(), "DAY_DISCUSSION, DAY_VOTING, or NIGHT_WOLVES");
        }

        ChatMessage message = new ChatMessage();
        message.setGameId(gameId);
        message.setSenderUserId(currentUser.getId());
        message.setChannel(channel);
        message.setContent(request.getContent());
        message.setCreatedAt(Instant.now());

        message = chatMessageRepository.save(message);

        AuditLogger.logChatMessage(gameId, currentUser.getId(), currentUser.getUsername(),
            channel.toString(), request.getContent() != null ? request.getContent().length() : 0);
        logger.info("Chat message saved: gameId={}, senderId={}, channel={}, messageId={}",
            gameId, currentUser.getId(), channel, message.getId());
    }

    private void createSystemMessage(Long gameId, String content) {
        logger.debug("Creating system message: gameId={}, contentLength={}",
            gameId, content != null ? content.length() : 0);

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

    private Long getWolfVictimId(Game game) {
        List<GameAction> wolfVotes = gameActionRepository.findByGameIdAndDayNumberAndPhaseAndActionType(
                game.getId(), game.getDayNumber(), GamePhase.NIGHT_WOLVES, ActionType.VOTE_WOLF_KILL);
        return getMajorityTarget(wolfVotes);
    }

    @Transactional(readOnly = true)
    public WolfVictimDto getWolfVictim(Long gameId, User currentUser) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", gameId));

        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("You are not in this game", currentUser.getId(), "GET_WOLF_VICTIM"));

        RoleTemplate role = roleTemplateRepository.findById(player.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", player.getRoleId()));

        if (role.getName() != RoleName.WITCH) {
            throw new UnauthorizedActionException("Only the Witch can see the wolf victim", currentUser.getId(), "GET_WOLF_VICTIM");
        }

        if (game.getCurrentPhase() != GamePhase.NIGHT_WITCH) {
            throw new InvalidGameStateException("Can only see victim during Witch phase",
                game.getCurrentPhase().toString(), GamePhase.NIGHT_WITCH.toString());
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
                .orElseThrow(() -> new UnauthorizedActionException("You are not in this game", currentUser.getId(), "GET_INSPECTION_RESULT"));

        RoleTemplate role = roleTemplateRepository.findById(player.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", player.getRoleId()));

        if (role.getName() != RoleName.SEER) {
            throw new UnauthorizedActionException("Only the Seer can see inspection results", currentUser.getId(), "GET_INSPECTION_RESULT");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", gameId));

        List<GameAction> inspections = gameActionRepository.findByGameIdAndActorPlayerIdAndActionType(
                game.getId(), player.getId(), ActionType.SEER_INSPECT);

        inspections = inspections.stream()
                .filter(a -> a.getTargetPlayerId() != null)
                .sorted(Comparator.comparing(GameAction::getCreatedAt).reversed())
                .collect(Collectors.toList());

        if (inspections.isEmpty()) {
            return null;
        }

        GameAction lastInspection = inspections.get(0);

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


