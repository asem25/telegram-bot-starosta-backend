package ru.semavin.telegrambot.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.dto.NotificationHistoryResponse;
import ru.semavin.telegrambot.dto.NotificationSettingsResponse;
import ru.semavin.telegrambot.dto.UpdateNotificationSettingsRequest;
import ru.semavin.telegrambot.models.NotificationHistoryEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.repositories.NotificationHistoryRepository;
import ru.semavin.telegrambot.repositories.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationPreferencesServiceTest {
    private UserRepository userRepository;
    private NotificationHistoryRepository historyRepository;
    private NotificationPreferencesService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        historyRepository = mock(NotificationHistoryRepository.class);
        service = new NotificationPreferencesService(userRepository, historyRepository);
        user = UserEntity.builder()
                .id(11L)
                .telegramId(1001L)
                .build();
        when(userRepository.findByTelegramId(1001L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void savesPreferencesAndReturnsThemOnNextRead() {
        NotificationSettingsResponse updated = service.updateSettings(
                1001L,
                new UpdateNotificationSettingsRequest(false, true, true));

        assertFalse(updated.scheduleChangesEnabled());
        assertTrue(updated.deadlineRemindersEnabled());
        assertTrue(updated.telegramWriteAccessGranted());
        assertEquals(updated, service.getSettings(1001L));
        verify(userRepository).save(user);
    }

    @Test
    void historyDoesNotDependOnSuccessfulTelegramDelivery() {
        UUID eventId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-30T10:15:30+03:00");
        NotificationHistoryEntity event = NotificationHistoryEntity.builder()
                .eventId(eventId)
                .user(user)
                .type("DEADLINE")
                .title("Срок приближается")
                .body("До дедлайна остался день")
                .createdAt(createdAt)
                .telegramDeliveryStatus("TEMPORARY_FAILURE")
                .build();
        when(historyRepository.findTop100ByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(event));

        List<NotificationHistoryResponse> history = service.getHistory(1001L);

        assertEquals(1, history.size());
        assertEquals(eventId, history.getFirst().id());
        assertEquals("TEMPORARY_FAILURE", history.getFirst().telegramDeliveryStatus());
    }

    @Test
    void synchronizesDeniedTelegramPermissionWithoutShowingItAsActive() {
        user.setTelegramWriteAccessGranted(true);

        service.synchronizeTelegramWriteAccess(1001L, false);

        assertFalse(service.getSettings(1001L).telegramWriteAccessGranted());
        verify(userRepository).save(user);
    }
}
