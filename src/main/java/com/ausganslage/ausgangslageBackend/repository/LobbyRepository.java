package com.ausganslage.ausgangslageBackend.repository;

import com.ausganslage.ausgangslageBackend.model.Lobby;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LobbyRepository extends JpaRepository<Lobby, Long> {
    Optional<Lobby> findByLobbyCode(String lobbyCode);
}

