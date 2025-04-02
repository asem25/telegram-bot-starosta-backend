package ru.semavin.telegrambot.services.groups;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.GroupDTO;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.mapper.GroupMapper;
import ru.semavin.telegrambot.mapper.UserMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.repositories.GroupRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.GroupNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.UserAlreadyExistsForStarostaException;
import ru.semavin.telegrambot.utils.exceptions.UserAlreadyNotExistsForStarostaException;
import ru.semavin.telegrambot.utils.exceptions.UserNotFoundException;

import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;
    private final UserRepository userRepository;
    private final GroupParserService groupParserService;
    private final UserMapper userMapper;

    @PostConstruct
    @Transactional
    public void init() {
        groupParserService.findAllGroups().forEach(group -> {
            if (groupRepository.findByGroupNameIgnoreCase(group.getGroupName()).isEmpty()) {
                groupRepository.save(group);
            }
        });
    }

    public GroupDTO findDtoByName(String name) {
        GroupEntity group = groupRepository.findByGroupNameIgnoreCase(name)
                .orElseThrow(() -> ExceptionFabric.create(GroupNotFoundException.class, ExceptionMessages.GROUP_NOT_FOUND));
        GroupDTO groupDTO = groupMapper.groupToDTO(group);
        if (group.getUsers().isEmpty())
            return groupDTO;

        groupDTO.setNames_students(group.getUsers().stream().map(UserEntity::getUsername).toList());
        return groupDTO;
    }
    public GroupEntity findEntityByName(String name) {
        return groupRepository.findByGroupNameIgnoreCase(name)
                .orElseThrow(() -> ExceptionFabric.create(GroupNotFoundException.class, ExceptionMessages.GROUP_NOT_FOUND));
    }
    public GroupDTO findDtoById(Long id) {
        return groupMapper.groupToDTO(groupRepository.findById(id)
                .orElseThrow(() -> ExceptionFabric.create(GroupNotFoundException.class, ExceptionMessages.GROUP_NOT_FOUND)));
    }
    @Transactional
    public GroupDTO setStarosta(String groupName, String starostaUsername) {
        GroupEntity group = findEntityByName(groupName);
        if (group.getStarosta() != null){
            throw ExceptionFabric.create(UserAlreadyExistsForStarostaException.class, ExceptionMessages.USER_ALREADY_EXISTS_FOR_STAROSTA);
        }
        UserEntity userEntity = userRepository.findByUsername(starostaUsername).orElseThrow(() -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND));

        group.setStarosta(userEntity);
        userEntity.setGroup(group);
        userEntity.setRole(UserRole.STAROSTA);

        userRepository.save(userEntity);
        return groupMapper.groupToDTO(groupRepository.save(group));
    }

    @Transactional
    public GroupDTO delStarosta(String groupName, String starostaUsername) {
        GroupEntity group = findEntityByName(groupName);
        if (group.getStarosta() == null) {
            throw ExceptionFabric.create(UserAlreadyNotExistsForStarostaException.class, ExceptionMessages.USER_NOW_NOT_EXISTS_AS_STAROSTA);
        }
        UserEntity userEntity = userRepository.findByUsername(starostaUsername).orElseThrow(() -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND));

        group.setStarosta(null);
        userEntity.setGroup(null);
        userEntity.setRole(UserRole.STUDENT);

        userRepository.save(userEntity);
        return groupMapper.groupToDTO(groupRepository.save(group));
    }
    @Transactional
    public GroupDTO createGroup(GroupDTO groupDTO) {
        GroupEntity group = GroupEntity.builder()
                .groupName(groupDTO.getGroupName())
                .users(new ArrayList<>())
                .build();
        groupRepository.save(group);
        return groupMapper.groupToDTO(group);
    }

    public UserDTO getStarosta(String groupName) {
        GroupEntity group = findEntityByName(groupName);
        UserEntity userEntity = group.getStarosta();
        if (userEntity == null) {
            ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND);
        }
        return userMapper.userToUserDTO(userEntity);
    }
}
