package ru.semavin.telegrambot.repositories;

import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByTelegramId(Long telegramId);
    boolean existsByTelegramId(Long telegramId);

    Optional<UserEntity> findByFirstNameAndLastNameAndPatronymicIgnoreCase(String firstName, String lastName, String patronymic);

    boolean existsByFirstNameAndLastNameAndPatronymicIgnoreCase(String firstName, String lastName, String patronymic);

    Optional<UserEntity> findByTeacherUuid(String teacherUuid);
}
