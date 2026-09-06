package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.NotificationHistoryResponse;
import ru.semavin.telegrambot.dto.NotificationSettingsResponse;
import ru.semavin.telegrambot.dto.UpdateNotificationSettingsRequest;
import ru.semavin.telegrambot.models.NotificationHistoryEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.repositories.NotificationHistoryRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.UserNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationPreferencesService {
    private final UserRepository userRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings(long userId) {
        return toSettings(requireUser(userId));
    }

    @Transactional
    public NotificationSettingsResponse updateSettings(
            long userId,
            UpdateNotificationSettingsRequest request
    ) {
        UserEntity user = requireUser(userId);
        user.setScheduleNotificationsEnabled(request.scheduleChangesEnabled());
        user.setDeadlineNotificationsEnabled(request.deadlineRemindersEnabled());
        user.setTelegramWriteAccessGranted(request.telegramWriteAccessGranted());
        return toSettings(userRepository.save(user));
    }

    @Transactional
    public void synchronizeTelegramWriteAccess(long telegramId, boolean granted) {
        UserEntity user = requireTelegramUser(telegramId);
        if (user.isTelegramWriteAccessGranted() != granted) {
            user.setTelegramWriteAccessGranted(granted);
            userRepository.save(user);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationHistoryResponse> getHistory(long userId) {
        return notificationHistoryRepository.findTop100ByUserOrderByCreatedAtDesc(requireUser(userId))
                .stream()
                .map(this::toHistory)
                .toList();
    }

    private UserEntity requireUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ExceptionFabric.create(
                        UserNotFoundException.class,
                        ExceptionMessages.USER_NOT_FOUND));
    }

    private UserEntity requireTelegramUser(long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> ExceptionFabric.create(
                        UserNotFoundException.class,
                        ExceptionMessages.USER_NOT_FOUND));
    }

    private NotificationSettingsResponse toSettings(UserEntity user) {
        return new NotificationSettingsResponse(
                user.isScheduleNotificationsEnabled(),
                user.isDeadlineNotificationsEnabled(),
                user.isTelegramWriteAccessGranted());
    }

    private NotificationHistoryResponse toHistory(NotificationHistoryEntity event) {
        return new NotificationHistoryResponse(
                event.getEventId(),
                event.getType(),
                event.getTitle(),
                event.getBody(),
                event.getCreatedAt(),
                event.getTelegramDeliveryStatus());
    }
}
