package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.mapper.UserMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.UserNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.UserWithTelegramIdAlreadyExistsException;

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
        UserEntity saved = userRepository.save(userEntity);
        log.info("User saved: {}", saved);
        return saved.getUsername();
    }
    @Transactional
    public String update(UserDTO user){
        log.info("Updating user: {}", user);
        UserEntity userBeforeSave = userRepository.findByTelegramId(user.getTelegramId())
                .orElseThrow(() -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND));
        GroupEntity group = groupService.findEntityByName(user.getGroupName());

        userBeforeSave.setUsername(user.getUsername());
        userBeforeSave.setGroup(group);

        UserEntity userAfterSave = userRepository.save(userBeforeSave);
        return String.format("""
                newUserName = %s,
                newGroupName = %s
                """, userAfterSave.getUsername(), userAfterSave.getGroup().getGroupName());
    }
    public UserDTO getUserEntity(String username){
        return userMapper.userToUserDTO(userRepository.findByUsername(username).orElseThrow(() -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND)));
    }

}
