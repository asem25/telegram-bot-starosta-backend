package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.semavin.telegrambot.models.DeadlineEntity;
import ru.semavin.telegrambot.models.GroupEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeadlineRepository extends JpaRepository<DeadlineEntity, Long> {
    List<DeadlineEntity> findAllByGroup(GroupEntity group);

    void deleteByUuid(UUID uuid);

    Optional<DeadlineEntity> findByUuid(UUID uuid);

    @Query("SELECT d FROM DeadlineEntity d WHERE d.dueDate BETWEEN :from AND :to")
    List<DeadlineEntity> findAllByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
