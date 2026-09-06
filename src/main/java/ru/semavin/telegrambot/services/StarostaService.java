package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.mapper.UserMapper;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.repositories.GroupRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRequestException;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRoleConflictException;
import ru.semavin.telegrambot.utils.exceptions.UserNotFoundException;

@Service
@RequiredArgsConstructor
public class StarostaService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDTO claimOwnGroup(Long userId) {
        int claimed = groupRepository.claimStarostaForOwnGroup(userId);
        UserEntity user = requireUser(userId);
        requireMembership(user);

        if (claimed == 0) {
            if (user.getGroup().getStarosta() != null
                    && user.getGroup().getStarosta().getId().equals(user.getId())) {
                return userMapper.userToUserDTO(user);
            }
            throw new MiniAppRoleConflictException("В учебной группе уже назначен староста");
        }

        user.setRole(UserRole.STAROSTA);
        return userMapper.userToUserDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO releaseOwnGroup(Long userId) {
        int released = groupRepository.releaseStarostaForOwnGroup(userId);
        UserEntity user = requireUser(userId);
        requireMembership(user);

        if (released == 0) {
            throw new MiniAppRoleConflictException("Пользователь не является старостой своей группы");
        }

        user.setRole(UserRole.STUDENT);
        return userMapper.userToUserDTO(userRepository.save(user));
    }

    private UserEntity requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ExceptionFabric.create(
                        UserNotFoundException.class,
                        ExceptionMessages.USER_NOT_FOUND));
    }

    private void requireMembership(UserEntity user) {
        if (user.getGroup() == null) {
            throw new MiniAppRequestException("Сначала выберите учебную группу");
        }
    }
}
