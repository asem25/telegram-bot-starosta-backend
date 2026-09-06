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
    void provisionsByTelegramIdOnlyAndReturnsInternalIdWithSafeProfile() {
        EntityManager entityManager = mock(EntityManager.class);
        UserRepository repository = mock(UserRepository.class);
        UserMapper mapper = mock(UserMapper.class);
        GroupService groupService = mock(GroupService.class);
        UserService service = new UserService(entityManager, repository, mapper, groupService);

        long telegramId = 42L;
        long userId = 7L;
        GroupEntity existingGroup = mock(GroupEntity.class);
        UserEntity existing = UserEntity.builder()
                .id(userId)
                .telegramId(telegramId)
                .role(UserRole.STAROSTA)
                .group(existingGroup)
                .build();
        UserDTO mapped = UserDTO.builder()
                .role("STAROSTA")
                .groupName("TEST-GROUP")
                .build();
        when(repository.findByTelegramId(telegramId)).thenReturn(Optional.of(existing));
        when(mapper.userToUserDTO(existing)).thenReturn(mapped);

        UserService.ProvisionedUser result = service.provisionTelegramUser(telegramId);

        verify(repository).upsertTelegramUser(telegramId);
        assertEquals(userId, result.userId());
        assertEquals("STAROSTA", result.profile().getRole());
        assertEquals("TEST-GROUP", result.profile().getGroupName());
    }
}
