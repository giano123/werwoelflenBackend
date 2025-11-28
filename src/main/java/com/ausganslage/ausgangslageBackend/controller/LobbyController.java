package com.ausganslage.ausgangslageBackend.controller;

import com.ausganslage.ausgangslageBackend.dto.CreateLobbyRequest;
import com.ausganslage.ausgangslageBackend.dto.LobbyStateDto;
import com.ausganslage.ausgangslageBackend.model.Game;
import com.ausganslage.ausgangslageBackend.model.User;
import com.ausganslage.ausgangslageBackend.service.GameService;
import com.ausganslage.ausgangslageBackend.service.LobbyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {

    private final LobbyService lobbyService;
    private final GameService gameService;

    public LobbyController(LobbyService lobbyService, GameService gameService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<LobbyStateDto> createLobby(@RequestBody CreateLobbyRequest request,
                                                      @RequestAttribute("currentUser") User currentUser) {
        LobbyStateDto lobby = lobbyService.createLobby(request, currentUser);
        return ResponseEntity.ok(lobby);
    }

    @GetMapping("/{code}/state")
    public ResponseEntity<LobbyStateDto> getLobbyState(@PathVariable String code) {
        try {
            LobbyStateDto state = lobbyService.getLobbyState(code);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<LobbyStateDto> joinLobby(@PathVariable String code,
                                                    @RequestAttribute("currentUser") User currentUser) {
        try {
            LobbyStateDto state = lobbyService.joinLobby(code, currentUser);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{code}/leave")
    public ResponseEntity<Void> leaveLobby(@PathVariable String code,
                                            @RequestAttribute("currentUser") User currentUser) {
        try {
            lobbyService.leaveLobby(code, currentUser);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{code}/ready")
    public ResponseEntity<Void> setReady(@PathVariable String code,
                                          @RequestParam(defaultValue = "true") boolean ready,
                                          @RequestAttribute("currentUser") User currentUser) {
        try {
            lobbyService.setReady(code, currentUser, ready);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{code}/start")
    public ResponseEntity<Game> startGame(@PathVariable String code,
                                          @RequestAttribute("currentUser") User currentUser) {
        try {
            Game game = gameService.startGame(code, currentUser);
            return ResponseEntity.ok(game);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

