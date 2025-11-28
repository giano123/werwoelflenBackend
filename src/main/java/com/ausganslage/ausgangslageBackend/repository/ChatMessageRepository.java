package com.ausganslage.ausgangslageBackend.repository;

import com.ausganslage.ausgangslageBackend.enums.ChatChannel;
import com.ausganslage.ausgangslageBackend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByLobbyIdAndCreatedAtAfterOrderByCreatedAt(Long lobbyId, Instant since);
    List<ChatMessage> findByGameIdAndChannelAndCreatedAtAfterOrderByCreatedAt(Long gameId, ChatChannel channel, Instant since);
    List<ChatMessage> findByGameIdAndChannelInAndCreatedAtAfterOrderByCreatedAt(Long gameId, List<ChatChannel> channels, Instant since);
}

