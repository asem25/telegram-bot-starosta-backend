package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final GroupService groupService;
    @Transactional
    public String save(UserDTO user){
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
        }else {
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
    public UserDTO getUserEntity(String username){
        return userMapper.userToUserDTO(userRepository.findByUsername(username)
                .orElseThrow(() -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND)));
    }

    @Transactional
    public UserEntity findOrCreateTeacherAndAddGroup(String teacherUuid, String teacherName, GroupEntity group) {
        if (teacherUuid.equals("00000000-0000-0000-0000-000000000000")) {
            Optional<UserEntity> user = userRepository.findByTeacherUuid(teacherUuid);
            return user.orElseGet(() -> userRepository.save(
                    UserEntity.builder()
                            .teacherUuid(teacherUuid)
                            .role(UserRole.TEACHER)
                            .firstName("Не указан")
                            .lastName(" ")
                            .patronymic(" ")
                            .build()
            ));
        }
        String[] teacherFullName = teacherName.split(" ");
        String lastName = teacherFullName[0];
        String firstName = teacherFullName[1];
        String patronymic = teacherFullName[2];


        Optional<UserEntity> teacher = userRepository.findByFirstNameAndLastNameAndPatronymicIgnoreCase(firstName, lastName, patronymic);
        if (teacher.isPresent()) {
            if (!teacher.get().getTeachingGroups().contains(group)) {
                teacher.get().getTeachingGroups().add(group);
            }
            ;
            return teacher.get();
        }

        UserEntity user = UserEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .patronymic(patronymic)
                .teacherUuid(teacherUuid)
                .role(UserRole.TEACHER)
                .build();

        user.getTeachingGroups().add(group);

        return userRepository.save(user);
    }

    public UserEntity findTeacher(String teacherUuid) {
        return null;
    }

}
