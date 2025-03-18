package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
