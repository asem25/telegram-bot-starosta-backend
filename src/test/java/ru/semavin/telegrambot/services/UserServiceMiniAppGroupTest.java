package ru.semavin.telegrambot.services;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.mapper.UserMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.services.groups.GroupService;
import ru.semavin.telegrambot.utils.exceptions.MiniAppGroupConflictException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceMiniAppGroupTest {
    private UserRepository userRepository;
    private UserMapper userMapper;
    private GroupService groupService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = mock(UserMapper.class);
        groupService = mock(GroupService.class);
        userService = new UserService(
                mock(EntityManager.class),
                userRepository,
                userMapper,
                groupService);
    }

    @Test
    void assignsNormalizedGroupWhenUserHasNoGroup() {
        GroupEntity group = GroupEntity.builder().id(7L).groupName("GROUP-1").build();
        UserEntity user = UserEntity.builder().telegramId(123L).group(group).build();
        UserDTO expected = UserDTO.builder().telegramId(123L).groupName("GROUP-1").build();
        when(groupService.findEntityByName("group-1")).thenReturn(group);
        when(userRepository.assignInitialGroup(123L, 7L)).thenReturn(1);
        when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
        when(userMapper.userToUserDTO(user)).thenReturn(expected);

        UserDTO actual = userService.assignInitialGroup(123L, "  group-1  ");

        assertSame(expected, actual);
        verify(groupService).findEntityByName("group-1");
    }

    @Test
    void repeatedSelectionOfSameGroupIsIdempotent() {
        GroupEntity group = GroupEntity.builder().id(7L).groupName("GROUP-1").build();
        UserEntity user = UserEntity.builder().telegramId(123L).group(group).build();
        UserDTO expected = UserDTO.builder().telegramId(123L).groupName("GROUP-1").build();
        when(groupService.findEntityByName("GROUP-1")).thenReturn(group);
        when(userRepository.assignInitialGroup(123L, 7L)).thenReturn(0);
        when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
        when(userMapper.userToUserDTO(user)).thenReturn(expected);

        assertSame(expected, userService.assignInitialGroup(123L, "GROUP-1"));
    }

    @Test
    void rejectsReplacingPreviouslySelectedGroup() {
        GroupEntity requestedGroup = GroupEntity.builder().id(7L).groupName("GROUP-1").build();
        GroupEntity currentGroup = GroupEntity.builder().id(8L).groupName("GROUP-2").build();
        UserEntity user = UserEntity.builder().telegramId(123L).group(currentGroup).build();
        when(groupService.findEntityByName("GROUP-1")).thenReturn(requestedGroup);
        when(userRepository.assignInitialGroup(123L, 7L)).thenReturn(0);
        when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));

        assertThrows(MiniAppGroupConflictException.class,
                () -> userService.assignInitialGroup(123L, "GROUP-1"));
    }
}
