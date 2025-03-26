package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.RequestEntity;

public interface RequestRepository extends JpaRepository<RequestEntity, Long> {
}
