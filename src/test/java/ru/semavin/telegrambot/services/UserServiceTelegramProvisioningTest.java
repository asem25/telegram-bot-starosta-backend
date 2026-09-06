package ru.semavin.telegrambot.services;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.mapper.UserMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTelegramProvisioningTest {

    @Test
    void returnsExistingRoleAndGroupAfterRefreshingTelegramProfile() {
        EntityManager entityManager = mock(EntityManager.class);
        UserRepository repository = mock(UserRepository.class);
        UserMapper mapper = mock(UserMapper.class);
        GroupService groupService = mock(GroupService.class);
        UserService service = new UserService(entityManager, repository, mapper, groupService);

        long telegramId = 42L;
        GroupEntity existingGroup = mock(GroupEntity.class);
        UserEntity existing = UserEntity.builder()
                .telegramId(telegramId)
                .username("new_name")
                .firstName("New")
                .lastName("Profile")
                .role(UserRole.STAROSTA)
                .group(existingGroup)
                .build();
        UserDTO mapped = UserDTO.builder()
                .telegramId(telegramId)
                .username("new_name")
                .role("STAROSTA")
                .groupName("TEST-GROUP")
                .build();
        when(repository.findByTelegramId(telegramId)).thenReturn(Optional.of(existing));
        when(mapper.userToUserDTO(existing)).thenReturn(mapped);

        UserDTO result = service.provisionTelegramUser(
                telegramId, "new_name", "New", "Profile");

        verify(repository).upsertTelegramUser(
                telegramId, "new_name", "New", "Profile");
        assertEquals("STAROSTA", result.getRole());
        assertEquals("TEST-GROUP", result.getGroupName());
    }
}
