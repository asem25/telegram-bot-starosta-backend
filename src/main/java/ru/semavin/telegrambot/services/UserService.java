package ru.semavin.telegrambot.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.semavin.telegrambot.utils.exceptions.UserWithTelegramIdAlreadyExistsException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final EntityManager em;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final GroupService groupService;

    @Transactional
    public String save(UserDTO user) {
        log.info("Saving user: {}", user);
        if (userRepository.existsByTelegramId(user.getTelegramId())) {
            throw ExceptionFabric.create(UserWithTelegramIdAlreadyExistsException.class, ExceptionMessages.USER_TELEGRAM_ID_EXISTS);
        }
        UserEntity userEntity = userMapper.userDTOToUser(user);
        log.info("User after mapping: {}", userEntity);
        // Если DTO содержит groupName (не пустой/не null)
        if (user.getGroupName() != null) {
            // Допустим, вы ищете группу по названию
            GroupEntity group = groupService.findEntityByName(user.getGroupName());
            userEntity.setGroup(group);
        } else {
            // Если DTO groupName == null, значит пользователь без группы
            userEntity.setGroup(null);
        }
        // Если не назначена роль(не учитель, то ставим студента)
        if (userEntity.getRole() == null)
            userEntity.setRole(UserRole.STUDENT);
        UserEntity saved = userRepository.save(userEntity);
        log.info("User saved: {}", saved.getUsername());
        return saved.getUsername();
    }

    @Transactional
    public String update(UserDTO user) {
        UserEntity userEntity = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND));

        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());
        userEntity.setGroup(groupService.findEntityByName(user.getGroupName()));

        return userRepository.save(userEntity).getUsername();
    }

    public UserDTO getUserEntity(String username) {
        return userMapper.userToUserDTO(userRepository.findByUsername(username)
                .orElseThrow(() -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND)));
    }

    @Transactional
    public UserEntity saveEntity(UserEntity user) {
       userRepository.insertWithConflict(
               user.getFirstName(),
               user.getLastName(),
               user.getPatronymic(),
               user.getRole().name(),
               user.getTeacherUuid(),
               null,
               null,
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

}
