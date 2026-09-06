package ru.semavin.telegrambot.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.mapper.UserMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.services.groups.GroupService;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.UserNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.MiniAppGroupConflictException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final EntityManager em;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final GroupService groupService;

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long userId) {
        return userMapper.userToUserDTO(findById(userId));
    }

    /**
     * Assigns a group once for a Telegram Mini App user. The conditional update
     * prevents two concurrent requests from silently replacing one another.
     */
    @Transactional
    public UserDTO assignInitialGroup(Long userId, String requestedGroupName) {
        String normalizedGroupName = requestedGroupName.trim();
        GroupEntity requestedGroup = groupService.findEntityByName(normalizedGroupName);

        int updated = userRepository.assignInitialGroup(userId, requestedGroup.getId());
        UserEntity currentUser = userRepository.findById(userId)
                .orElseThrow(() -> ExceptionFabric.create(
                        UserNotFoundException.class,
                        ExceptionMessages.USER_NOT_FOUND));

        if (updated == 0 && (currentUser.getGroup() == null
                || !currentUser.getGroup().getId().equals(requestedGroup.getId()))) {
            throw new MiniAppGroupConflictException("Учебная группа уже выбрана и не может быть изменена");
        }

        return userMapper.userToUserDTO(currentUser);
    }

    /**
     * Registers a pseudonymous user on first valid Telegram Mini App login.
     * Telegram profile fields are deliberately ignored.
     */
    @Transactional
    public ProvisionedUser provisionTelegramUser(Long telegramId) {
        userRepository.upsertTelegramUser(telegramId);
        UserEntity user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> ExceptionFabric.create(
                        UserNotFoundException.class,
                        ExceptionMessages.USER_NOT_FOUND));
        return new ProvisionedUser(user.getId(), userMapper.userToUserDTO(user));
    }

    @Transactional(readOnly = true)
    public UserEntity findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ExceptionFabric.create(
                        UserNotFoundException.class,
                        ExceptionMessages.USER_NOT_FOUND));
    }

    @Transactional
    public UserEntity saveEntity(UserEntity user) {
       userRepository.insertWithConflict(
               user.getFirstName(),
               user.getLastName(),
               user.getPatronymic(),
               user.getRole().name(),
               user.getTeacherUuid(),
               null
       );

       val id = userRepository.findByTeacherUuid(user.getTeacherUuid())
               .get().getId();

       user.getTeachingGroups().forEach(group ->
               userRepository.insertIgnore(id, group.getId()));

       return em.getReference(UserEntity.class, id);
    }

    @Transactional
    public synchronized UserEntity findOrCreateTeacherAndAddGroup(
            String teacherUuid,
            String teacherName,
            GroupEntity group
    ) {
        if ("00000000-0000-0000-0000-000000000000".equals(teacherUuid)) {
            return userRepository.findByTeacherUuid(teacherUuid)
                    .orElseGet(() -> userRepository.save(
                            UserEntity.builder()
                                    .teacherUuid(teacherUuid)
                                    .role(UserRole.TEACHER)
                                    .firstName("Не указан")
                                    .lastName(" ")
                                    .patronymic(" ")
                                    .build()
                    ));
        }

        UserEntity teacher = userRepository.findByTeacherUuid(teacherUuid)
                .orElseGet(() -> createUser(teacherUuid, teacherName));

        userRepository.insertIgnore(
                teacher.getId(),
                group.getId()
        );

        return teacher;
    }

    private UserEntity createUser(String teacherUuid, String teacherName) {
        String lastName = "";
        String firstName = "";
        String patronymic = "";

        if (teacherName != null && !teacherName.isBlank()) {
            String[] parts = teacherName.trim().split("\\s+");
            if (parts.length > 0) lastName = parts[0];
            if (parts.length > 1) firstName = parts[1];
            if (parts.length > 2) patronymic = parts[2];
        }

        return userRepository.saveAndFlush(
                UserEntity.builder()
                        .teacherUuid(teacherUuid)
                        .role(UserRole.TEACHER)
                        .firstName(firstName)
                        .lastName(lastName)
                        .patronymic(patronymic)
                        .build()
        );
    }

    public UserEntity findTeacher(String teacherUuid) {
        return userRepository.findByTeacherUuid(teacherUuid)
                .orElseThrow(() ->
                        ExceptionFabric.create(UserNotFoundException.class,
                                ExceptionMessages.USER_NOT_FOUND));
    }

    public record ProvisionedUser(long userId, UserDTO profile) {
    }

}
