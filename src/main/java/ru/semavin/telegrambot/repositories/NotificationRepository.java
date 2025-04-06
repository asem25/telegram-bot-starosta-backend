package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.NotificationEntity;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    void deleteByUuid(UUID uuid);

    List<NotificationEntity> findAllByGroupName(GroupEntity group);
}
