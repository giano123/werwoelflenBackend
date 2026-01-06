package com.ausganslage.ausgangslageBackend.service;

import com.ausganslage.ausgangslageBackend.dto.*;
import com.ausganslage.ausgangslageBackend.enums.LobbyStatus;
import com.ausganslage.ausgangslageBackend.model.*;
import com.ausganslage.ausgangslageBackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;
    @Mock
    private LobbyMemberRepository lobbyMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private LobbyService lobbyService;

    private User user1;
    private User user2;
    private Lobby lobby;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("User1");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("User2");

        lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        lobby.setHostUserId(1L);
        lobby.setStatus(LobbyStatus.OPEN);
        lobby.setMaxPlayers(8);
        lobby.setCreatedAt(Instant.now());
    }

    @Test
    void testCreateLobby_Success() {
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> {
            Lobby l = invocation.getArgument(0);
            l.setId(1L);
            return l;
        });
        when(lobbyMemberRepository.save(any(LobbyMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        CreateLobbyRequest request = new CreateLobbyRequest();
        request.setMaxPlayers(6);

        LobbyStateDto result = lobbyService.createLobby(request, user1);

        assertNotNull(result);
        assertEquals(user1.getId(), result.getHostUserId());
        assertEquals(6, result.getMaxPlayers());
        assertNotNull(result.getLobbyCode());
        verify(lobbyRepository, times(1)).save(any(Lobby.class));
        verify(lobbyMemberRepository, times(1)).save(any(LobbyMember.class));
    }

    @Test
    void testJoinLobby_Success() {
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyId(1L)).thenReturn(new ArrayList<>());
        when(lobbyMemberRepository.findByLobbyIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(lobbyMemberRepository.save(any(LobbyMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        lobbyService.joinLobby("ABC123", user2);

        verify(lobbyMemberRepository, times(1)).save(argThat(member ->
            member.getUserId().equals(2L) && member.getLobbyId().equals(1L)
        ));
    }

    @Test
    void testJoinLobby_LobbyNotFound_ThrowsException() {
        when(lobbyRepository.findByLobbyCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            lobbyService.joinLobby("INVALID", user2)
        );
    }

    @Test
    void testJoinLobby_LobbyFull_ThrowsException() {
        lobby.setMaxPlayers(2);
        List<LobbyMember> members = Arrays.asList(
            createLobbyMember(1L, 1L),
            createLobbyMember(2L, 2L)
        );

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyId(1L)).thenReturn(members);

        User user3 = new User();
        user3.setId(3L);

        assertThrows(IllegalStateException.class, () ->
            lobbyService.joinLobby("ABC123", user3)
        );
    }

    @Test
    void testJoinLobby_AlreadyInLobby_ThrowsException() {
        LobbyMember existingMember = createLobbyMember(1L, 2L);

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyIdAndUserId(1L, 2L)).thenReturn(Optional.of(existingMember));

        assertThrows(IllegalStateException.class, () ->
            lobbyService.joinLobby("ABC123", user2)
        );
    }

    @Test
    void testLeaveLobby_Success() {
        LobbyMember member = createLobbyMember(1L, 2L);

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyIdAndUserId(1L, 2L)).thenReturn(Optional.of(member));
        when(lobbyMemberRepository.findByLobbyId(1L)).thenReturn(List.of(member));

        lobbyService.leaveLobby("ABC123", user2);

        verify(lobbyMemberRepository, times(1)).delete(member);
    }

    @Test
    void testLeaveLobby_HostLeaves_TransfersHost() {
        LobbyMember hostMember = createLobbyMember(1L, 1L);
        LobbyMember otherMember = createLobbyMember(2L, 2L);

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyIdAndUserId(1L, 1L)).thenReturn(Optional.of(hostMember));
        when(lobbyMemberRepository.findByLobbyId(1L)).thenReturn(Arrays.asList(hostMember, otherMember));

        lobbyService.leaveLobby("ABC123", user1);

        assertEquals(2L, lobby.getHostUserId());
        verify(lobbyRepository, times(1)).save(lobby);
        verify(lobbyMemberRepository, times(1)).delete(hostMember);
    }

    @Test
    void testLeaveLobby_LastPlayerLeaves_DeletesLobby() {
        LobbyMember member = createLobbyMember(1L, 1L);

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyIdAndUserId(1L, 1L)).thenReturn(Optional.of(member));
        when(lobbyMemberRepository.findByLobbyId(1L)).thenReturn(List.of(member));

        lobbyService.leaveLobby("ABC123", user1);

        verify(lobbyRepository, times(1)).delete(lobby);
        verify(lobbyMemberRepository, times(1)).delete(member);
    }

    @Test
    void testSetReady_Success() {
        LobbyMember member = createLobbyMember(1L, 2L);
        member.setIsReady(false);

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyIdAndUserId(1L, 2L)).thenReturn(Optional.of(member));

        lobbyService.setReady("ABC123", user2, true);

        assertTrue(member.getIsReady());
        verify(lobbyMemberRepository, times(1)).save(member);
    }


    @Test
    void testGetLobbyState_Success() {
        LobbyMember member1 = createLobbyMember(1L, 1L);
        LobbyMember member2 = createLobbyMember(2L, 2L);
        member2.setIsReady(true);

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyMemberRepository.findByLobbyId(1L)).thenReturn(Arrays.asList(member1, member2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        LobbyStateDto state = lobbyService.getLobbyState("ABC123");

        assertNotNull(state);
        assertEquals("ABC123", state.getLobbyCode());
        assertEquals(1L, state.getHostUserId());
        assertEquals(2, state.getMembers().size());
    }

    @Test
    void testGetLobbyState_LobbyNotFound_ThrowsException() {
        when(lobbyRepository.findByLobbyCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            lobbyService.getLobbyState("INVALID")
        );
    }

    private LobbyMember createLobbyMember(Long id, Long userId) {
        LobbyMember member = new LobbyMember();
        member.setId(id);
        member.setLobbyId(1L);
        member.setUserId(userId);
        member.setIsReady(false);
        member.setJoinedAt(Instant.now());
        return member;
    }
}

