package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.GroupEntity;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<GroupEntity, Long> {
    Optional<GroupEntity> findByGroupNameIgnoreCase(String groupName);

}
