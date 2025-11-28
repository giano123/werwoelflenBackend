package com.ausganslage.ausgangslageBackend.repository;

import com.ausganslage.ausgangslageBackend.model.LobbyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LobbyMemberRepository extends JpaRepository<LobbyMember, Long> {
    List<LobbyMember> findByLobbyId(Long lobbyId);
    Optional<LobbyMember> findByLobbyIdAndUserId(Long lobbyId, Long userId);
    void deleteByLobbyIdAndUserId(Long lobbyId, Long userId);
    long countByLobbyId(Long lobbyId);
}

