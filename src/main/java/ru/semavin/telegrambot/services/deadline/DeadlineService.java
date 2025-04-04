package ru.semavin.telegrambot.services.deadline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.DeadlineDTO;
import ru.semavin.telegrambot.mapper.DeadlineMapper;
import ru.semavin.telegrambot.models.DeadlineEntity;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.repositories.DeadlineRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.groups.GroupService;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.DeadlineNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.GroupNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.UserNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlineService {
    private final DeadlineRepository repository;
    private final DeadlineMapper mapper;
    private final GroupService groupService;
    private final UserRepository userRepository;

    public List<DeadlineDTO> getAllByGroup(String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);

        return mapper.toDtoList(repository.findAllByGroup(group));
    }

    public DeadlineDTO save(DeadlineDTO dto) {
        if (dto.getUsername() == null) {
            throw ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND);
        }
        if (dto.getGroupName() == null) {
            throw ExceptionFabric.create(GroupNotFoundException.class, ExceptionMessages.GROUP_NOT_FOUND);
        }
        DeadlineEntity entity = mapper.toEntity(dto);
        entity.setGroup(groupService.findEntityByName(dto.getGroupName()));
        entity.setCreator(userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND)));
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        repository.deleteByUuid(UUID.fromString(id));
    }

    public List<DeadlineDTO> getDeadlinesBetween(LocalDate from, LocalDate to) {
        return mapper.toDtoList(repository.findAllByDateRange(from, to));
    }

    @Transactional
    public void markNotified(String id, boolean notified3Days, boolean notified1Day) {
        DeadlineEntity deadline = repository.findByUuid(UUID.fromString(id))
                .orElseThrow(() -> ExceptionFabric.create(DeadlineNotFoundException.class, ExceptionMessages.DEADLINE_NOT_FOUND));
        deadline.setNotified3Days(notified3Days);
        deadline.setNotified1Day(notified1Day);
    }
}
