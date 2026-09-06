package ru.semavin.telegrambot.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.mapper.UserMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.repositories.GroupRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRequestException;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRoleConflictException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarostaServiceTest {
    private UserRepository userRepository;
    private GroupRepository groupRepository;
    private UserMapper userMapper;
    private StarostaService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        groupRepository = mock(GroupRepository.class);
        userMapper = mock(UserMapper.class);
        service = new StarostaService(userRepository, groupRepository, userMapper);
    }

    @Test
    void promotesOnlyAfterAtomicClaimSucceeds() {
        GroupEntity group = GroupEntity.builder().id(10L).build();
        UserEntity user = UserEntity.builder().id(1L).telegramId(100L)
                .group(group).role(UserRole.STUDENT).build();
        UserDTO expected = UserDTO.builder().role("STAROSTA").build();
        when(groupRepository.claimStarostaForOwnGroup(1L)).thenReturn(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserDTO(user)).thenReturn(expected);

        assertSame(expected, service.claimOwnGroup(1L));

        assertEquals(UserRole.STAROSTA, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void losingConcurrentClaimDoesNotPromoteUser() {
        UserEntity winner = UserEntity.builder().id(2L).build();
        GroupEntity group = GroupEntity.builder().id(10L).starosta(winner).build();
        UserEntity loser = UserEntity.builder().id(1L).telegramId(100L)
                .group(group).role(UserRole.STUDENT).build();
        when(groupRepository.claimStarostaForOwnGroup(1L)).thenReturn(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(loser));

        assertThrows(MiniAppRoleConflictException.class, () -> service.claimOwnGroup(1L));

        assertEquals(UserRole.STUDENT, loser.getRole());
        verify(userRepository, never()).save(loser);
    }

    @Test
    void userWithoutMembershipCannotClaimAnyGroup() {
        UserEntity user = UserEntity.builder().id(1L).telegramId(100L).role(UserRole.STUDENT).build();
        when(groupRepository.claimStarostaForOwnGroup(1L)).thenReturn(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(MiniAppRequestException.class, () -> service.claimOwnGroup(1L));
        verify(userRepository, never()).save(user);
    }

    @Test
    void releaseDemotesButPreservesMembership() {
        GroupEntity group = GroupEntity.builder().id(10L).build();
        UserEntity user = UserEntity.builder().id(1L).telegramId(100L)
                .group(group).role(UserRole.STAROSTA).build();
        UserDTO expected = UserDTO.builder().groupName("GROUP-1").role("STUDENT").build();
        when(groupRepository.releaseStarostaForOwnGroup(1L)).thenReturn(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserDTO(user)).thenReturn(expected);

        assertSame(expected, service.releaseOwnGroup(1L));

        assertSame(group, user.getGroup());
        assertEquals(UserRole.STUDENT, user.getRole());
    }

    @Test
    void cannotReleaseAnotherUsersRole() {
        GroupEntity group = GroupEntity.builder().id(10L).build();
        UserEntity user = UserEntity.builder().id(1L).telegramId(100L)
                .group(group).role(UserRole.STUDENT).build();
        when(groupRepository.releaseStarostaForOwnGroup(1L)).thenReturn(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(MiniAppRoleConflictException.class, () -> service.releaseOwnGroup(1L));
        verify(userRepository, never()).save(user);
    }
}
