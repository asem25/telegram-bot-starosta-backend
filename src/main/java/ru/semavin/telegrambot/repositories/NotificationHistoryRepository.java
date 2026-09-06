package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.NotificationHistoryEntity;
import ru.semavin.telegrambot.models.UserEntity;

import java.util.List;
import java.util.UUID;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistoryEntity, Long> {
    List<NotificationHistoryEntity> findTop100ByUserOrderByCreatedAtDesc(UserEntity user);

    boolean existsByEventId(UUID eventId);
}
