package com.ausganslage.ausgangslageBackend.repository;

import com.ausganslage.ausgangslageBackend.model.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
    List<GamePlayer> findByGameId(Long gameId);
    Optional<GamePlayer> findByGameIdAndUserId(Long gameId, Long userId);
    List<GamePlayer> findByGameIdAndIsAlive(Long gameId, Boolean isAlive);
    long countByGameIdAndIsAlive(Long gameId, Boolean isAlive);
}

