package com.ausganslage.ausgangslageBackend.repository;

import com.ausganslage.ausgangslageBackend.enums.ActionType;
import com.ausganslage.ausgangslageBackend.enums.GamePhase;
import com.ausganslage.ausgangslageBackend.model.GameAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GameActionRepository extends JpaRepository<GameAction, Long> {
    List<GameAction> findByGameIdAndDayNumberAndPhase(Long gameId, Integer dayNumber, GamePhase phase);
    Optional<GameAction> findByGameIdAndDayNumberAndPhaseAndActorPlayerId(Long gameId, Integer dayNumber, GamePhase phase, Long actorPlayerId);
    List<GameAction> findByGameIdAndDayNumberAndPhaseAndActionType(Long gameId, Integer dayNumber, GamePhase phase, ActionType actionType);
}

