package ru.semavin.telegrambot.services.groups;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.GroupDTO;
import ru.semavin.telegrambot.mapper.GroupMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.repositories.GroupRepository;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.GroupNotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;
    private final GroupParserService groupParserService;

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
        return groupMapper.groupToDTO(group);
    }

    public GroupEntity findEntityByName(String name) {
        return groupRepository.findByGroupNameIgnoreCase(name)
                .orElseThrow(() -> ExceptionFabric.create(GroupNotFoundException.class, ExceptionMessages.GROUP_NOT_FOUND));
    }

    public List<GroupEntity> findAll() {
        return groupRepository.findAll();
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

}
