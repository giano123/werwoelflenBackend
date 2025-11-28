package com.ausganslage.ausgangslageBackend.controller;

import com.ausganslage.ausgangslageBackend.dto.*;
import com.ausganslage.ausgangslageBackend.model.Game;
import com.ausganslage.ausgangslageBackend.model.Lobby;
import com.ausganslage.ausgangslageBackend.model.User;
import com.ausganslage.ausgangslageBackend.repository.GameRepository;
import com.ausganslage.ausgangslageBackend.repository.LobbyRepository;
import com.ausganslage.ausgangslageBackend.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final GameRepository gameRepository;
    private final LobbyRepository lobbyRepository;

    public GameController(GameService gameService, GameRepository gameRepository, LobbyRepository lobbyRepository) {
        this.gameService = gameService;
        this.gameRepository = gameRepository;
        this.lobbyRepository = lobbyRepository;
    }

    @PostMapping("/start/{lobbyCode}")
    public ResponseEntity<Game> startGame(@PathVariable String lobbyCode,
                                          @RequestAttribute("currentUser") User currentUser) {
        try {
            Game game = gameService.startGame(lobbyCode, currentUser);
            return ResponseEntity.ok(game);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{gameId}/state")
    public ResponseEntity<GameStateDto> getGameState(@PathVariable Long gameId,
                                                      @RequestAttribute("currentUser") User currentUser) {
        try {
            GameStateDto state = gameService.getGameState(gameId, currentUser);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/lobby/{lobbyCode}")
    public ResponseEntity<Game> getGameByLobby(@PathVariable String lobbyCode) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElse(null);
        if (lobby == null) {
            return ResponseEntity.notFound().build();
        }

        Game game = gameRepository.findByLobbyId(lobby.getId()).orElse(null);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameId}/actions/vote")
    public ResponseEntity<Void> submitVote(@PathVariable Long gameId,
                                            @RequestBody VoteActionRequest request,
                                            @RequestAttribute("currentUser") User currentUser) {
        try {
            gameService.submitVote(gameId, currentUser, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameId}/actions/power")
    public ResponseEntity<Void> submitPowerAction(@PathVariable Long gameId,
                                                   @RequestBody PowerActionRequest request,
                                                   @RequestAttribute("currentUser") User currentUser) {
        try {
            gameService.submitPowerAction(gameId, currentUser, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameId}/actions/skip")
    public ResponseEntity<Void> skipAction(@PathVariable Long gameId,
                                            @RequestAttribute("currentUser") User currentUser) {
        try {
            gameService.skipAction(gameId, currentUser);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{gameId}/chat")
    public ResponseEntity<List<ChatMessageDto>> getChatMessages(@PathVariable Long gameId,
                                                                  @RequestParam(required = false) Long since,
                                                                  @RequestAttribute("currentUser") User currentUser) {
        try {
            List<ChatMessageDto> messages = gameService.getChatMessages(gameId, currentUser, since);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{gameId}/chat")
    public ResponseEntity<ChatMessageDto> sendChatMessage(@PathVariable Long gameId,
                                                           @RequestBody ChatMessageRequest request,
                                                           @RequestAttribute("currentUser") User currentUser) {
        try {
            ChatMessageDto message = gameService.sendChatMessage(gameId, currentUser, request);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameId}/transition-to-voting")
    public ResponseEntity<Void> transitionToVoting(@PathVariable Long gameId,
                                                    @RequestAttribute("currentUser") User currentUser) {
        try {
            gameService.transitionToVoting(gameId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{gameId}/wolf-victim")
    public ResponseEntity<WolfVictimDto> getWolfVictim(@PathVariable Long gameId,
                                                        @RequestAttribute("currentUser") User currentUser) {
        try {
            WolfVictimDto victim = gameService.getWolfVictim(gameId, currentUser);
            return ResponseEntity.ok(victim);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{gameId}/inspection-result")
    public ResponseEntity<InspectionResultDto> getInspectionResult(@PathVariable Long gameId,
                                                                    @RequestAttribute("currentUser") User currentUser) {
        try {
            InspectionResultDto result = gameService.getLastInspectionResult(gameId, currentUser);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

