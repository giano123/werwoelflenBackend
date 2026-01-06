package com.ausganslage.ausgangslageBackend.service;

import com.ausganslage.ausgangslageBackend.dto.CreateLobbyRequest;
import com.ausganslage.ausgangslageBackend.dto.LobbyMemberDto;
import com.ausganslage.ausgangslageBackend.dto.LobbyStateDto;
import com.ausganslage.ausgangslageBackend.enums.LobbyStatus;
import com.ausganslage.ausgangslageBackend.model.Lobby;
import com.ausganslage.ausgangslageBackend.model.LobbyMember;
import com.ausganslage.ausgangslageBackend.model.User;
import com.ausganslage.ausgangslageBackend.repository.LobbyMemberRepository;
import com.ausganslage.ausgangslageBackend.repository.LobbyRepository;
import com.ausganslage.ausgangslageBackend.repository.UserRepository;
import com.ausganslage.ausgangslageBackend.util.AuditLogger;
import com.ausganslage.ausgangslageBackend.util.CodeGenerator;
import com.ausganslage.ausgangslageBackend.util.LoggingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LobbyService {

    private static final Logger logger = LoggerFactory.getLogger(LobbyService.class);

    private final LobbyRepository lobbyRepository;
    private final LobbyMemberRepository lobbyMemberRepository;
    private final UserRepository userRepository;

    public LobbyService(LobbyRepository lobbyRepository, LobbyMemberRepository lobbyMemberRepository, UserRepository userRepository) {
        this.lobbyRepository = lobbyRepository;
        this.lobbyMemberRepository = lobbyMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LobbyStateDto createLobby(CreateLobbyRequest request, User currentUser) {
        logger.info("Creating new lobby: hostUserId={}, hostUsername={}, maxPlayers={}",
            currentUser.getId(), currentUser.getUsername(), request.getMaxPlayers());
        LoggingContext.setAction("CREATE_LOBBY");
        LoggingContext.setUserId(currentUser.getId());
        LoggingContext.setUsername(currentUser.getUsername());

        Lobby lobby = new Lobby();
        lobby.setLobbyCode(generateUniqueLobbyCode());
        lobby.setHostUserId(currentUser.getId());
        lobby.setMaxPlayers(request.getMaxPlayers());
        lobby.setStatus(LobbyStatus.OPEN);
        lobby.setSettingsJson(request.getSettingsJson());
        lobby.setCreatedAt(Instant.now());

        lobby = lobbyRepository.save(lobby);

        LoggingContext.setLobbyId(lobby.getId());
        logger.debug("Lobby entity created: lobbyId={}, lobbyCode={}", lobby.getId(), lobby.getLobbyCode());

        LobbyMember member = new LobbyMember();
        member.setLobbyId(lobby.getId());
        member.setUserId(currentUser.getId());
        member.setIsHost(true);
        member.setIsReady(true);
        member.setJoinedAt(Instant.now());

        lobbyMemberRepository.save(member);

        AuditLogger.logLobbyCreated(lobby.getId(), lobby.getLobbyCode(),
            currentUser.getId(), currentUser.getUsername());
        logger.info("Lobby created successfully: lobbyId={}, lobbyCode={}, hostUserId={}",
            lobby.getId(), lobby.getLobbyCode(), currentUser.getId());

        return getLobbyState(lobby.getLobbyCode());
    }

    @Transactional(readOnly = true)
    public LobbyStateDto getLobbyState(String lobbyCode) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode)
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found"));

        List<LobbyMember> members = lobbyMemberRepository.findByLobbyId(lobby.getId());

        LobbyStateDto dto = new LobbyStateDto();
        dto.setId(lobby.getId());
        dto.setLobbyCode(lobby.getLobbyCode());
        dto.setHostUserId(lobby.getHostUserId());
        dto.setMaxPlayers(lobby.getMaxPlayers());
        dto.setStatus(lobby.getStatus());
        dto.setSettingsJson(lobby.getSettingsJson());
        dto.setMembers(members.stream().map(this::toLobbyMemberDto).collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public LobbyStateDto joinLobby(String lobbyCode, User currentUser) {
        logger.info("User attempting to join lobby: userId={}, username={}, lobbyCode={}",
            currentUser.getId(), currentUser.getUsername(), lobbyCode);
        LoggingContext.setAction("JOIN_LOBBY");
        LoggingContext.setUserId(currentUser.getId());
        LoggingContext.setUsername(currentUser.getUsername());

        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode)
                .orElseThrow(() -> {
                    logger.warn("Join lobby failed - lobby not found: lobbyCode={}", lobbyCode);
                    return new IllegalArgumentException("Lobby not found");
                });

        LoggingContext.setLobbyId(lobby.getId());

        if (lobby.getStatus() != LobbyStatus.OPEN) {
            logger.warn("Join lobby failed - lobby not open: lobbyId={}, lobbyCode={}, status={}",
                lobby.getId(), lobbyCode, lobby.getStatus());
            throw new IllegalStateException("Lobby is not open");
        }

        long currentMemberCount = lobbyMemberRepository.countByLobbyId(lobby.getId());
        if (currentMemberCount >= lobby.getMaxPlayers()) {
            logger.warn("Join lobby failed - lobby full: lobbyId={}, currentMembers={}, maxPlayers={}",
                lobby.getId(), currentMemberCount, lobby.getMaxPlayers());
            throw new IllegalStateException("Lobby is full");
        }

        if (lobbyMemberRepository.findByLobbyIdAndUserId(lobby.getId(), currentUser.getId()).isPresent()) {
            logger.warn("Join lobby failed - user already in lobby: userId={}, lobbyId={}",
                currentUser.getId(), lobby.getId());
            throw new IllegalStateException("Already in lobby");
        }

        LobbyMember member = new LobbyMember();
        member.setLobbyId(lobby.getId());
        member.setUserId(currentUser.getId());
        member.setIsHost(false);
        member.setIsReady(false);
        member.setJoinedAt(Instant.now());

        lobbyMemberRepository.save(member);

        AuditLogger.logPlayerJoinedLobby(lobby.getId(), lobbyCode,
            currentUser.getId(), currentUser.getUsername());
        logger.info("User joined lobby successfully: userId={}, username={}, lobbyId={}, lobbyCode={}",
            currentUser.getId(), currentUser.getUsername(), lobby.getId(), lobbyCode);

        return getLobbyState(lobbyCode);
    }

    @Transactional
    public void leaveLobby(String lobbyCode, User currentUser) {
        logger.info("User leaving lobby: userId={}, username={}, lobbyCode={}",
            currentUser.getId(), currentUser.getUsername(), lobbyCode);
        LoggingContext.setAction("LEAVE_LOBBY");
        LoggingContext.setUserId(currentUser.getId());
        LoggingContext.setUsername(currentUser.getUsername());

        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode)
                .orElseThrow(() -> {
                    logger.warn("Leave lobby failed - lobby not found: lobbyCode={}", lobbyCode);
                    return new IllegalArgumentException("Lobby not found");
                });

        LoggingContext.setLobbyId(lobby.getId());

        lobbyMemberRepository.deleteByLobbyIdAndUserId(lobby.getId(), currentUser.getId());

        AuditLogger.logPlayerLeftLobby(lobby.getId(), lobbyCode,
            currentUser.getId(), currentUser.getUsername());
        logger.info("User left lobby: userId={}, username={}, lobbyId={}",
            currentUser.getId(), currentUser.getUsername(), lobby.getId());

        long remainingMembers = lobbyMemberRepository.countByLobbyId(lobby.getId());
        if (remainingMembers == 0) {
            logger.info("Lobby empty after user left - deleting lobby: lobbyId={}, lobbyCode={}",
                lobby.getId(), lobbyCode);
            lobbyRepository.delete(lobby);
        } else if (lobby.getHostUserId().equals(currentUser.getId())) {
            logger.info("Host left lobby - transferring host role: lobbyId={}, oldHostId={}",
                lobby.getId(), currentUser.getId());
            List<LobbyMember> members = lobbyMemberRepository.findByLobbyId(lobby.getId());
            if (!members.isEmpty()) {
                LobbyMember newHost = members.get(0);
                newHost.setIsHost(true);
                lobbyMemberRepository.save(newHost);
                lobby.setHostUserId(newHost.getUserId());
                logger.info("Host role transferred: lobbyId={}, newHostId={}",
                    lobby.getId(), newHost.getUserId());
                lobbyRepository.save(lobby);
            }
        }
    }

    @Transactional
    public void setReady(String lobbyCode, User currentUser, boolean ready) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode)
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found"));

        LobbyMember member = lobbyMemberRepository.findByLobbyIdAndUserId(lobby.getId(), currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Not a member of this lobby"));

        member.setIsReady(ready);
        lobbyMemberRepository.save(member);
    }

    private String generateUniqueLobbyCode() {
        String code;
        do {
            code = CodeGenerator.generateLobbyCode();
        } while (lobbyRepository.findByLobbyCode(code).isPresent());
        return code;
    }

    private LobbyMemberDto toLobbyMemberDto(LobbyMember member) {
        User user = userRepository.findById(member.getUserId()).orElse(null);

        LobbyMemberDto dto = new LobbyMemberDto();
        dto.setId(member.getId());
        dto.setUserId(member.getUserId());
        dto.setUsername(user != null ? user.getUsername() : "Unknown");
        dto.setAvatarConfig(user != null ? user.getAvatarConfig() : "default");
        dto.setIsHost(member.getIsHost());
        dto.setIsReady(member.getIsReady());

        return dto;
    }
}

