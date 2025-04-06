package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.NotificationDTO;
import ru.semavin.telegrambot.mapper.NotificationMapper;
import ru.semavin.telegrambot.models.AbsenceEntity;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.NotificationEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.repositories.GroupRepository;
import ru.semavin.telegrambot.repositories.NotificationRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.utils.DateUtils;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.GroupNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.UserNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final NotificationMapper notificationMapper;
    private final List<UUID> toDelete = new CopyOnWriteArrayList<>();

    @Transactional
    @CacheEvict(value = "notifications", allEntries = true)
    public void add(NotificationDTO notificationDTO) {
        //TODO проверку на то, есть ли в базе сейчас
        UserEntity user = userRepository.findByUsername(notificationDTO.getUsername())
                .orElseThrow(() -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND));
        GroupEntity group = groupRepository.findByGroupNameIgnoreCase(notificationDTO.getGroupName())
                .orElseThrow(() -> ExceptionFabric.create(GroupNotFoundException.class, ExceptionMessages.GROUP_NOT_FOUND));

        NotificationEntity entity = notificationMapper.notificationDTOToNotificationEntity(notificationDTO);
        entity.setUsername(user);
        entity.setGroupName(group);
        log.debug("Сохранение в таблицу пропусков! {}", entity.getUuid());

        notificationRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "notifications", allEntries = true)
    public void deleteByUUID(String uuid) {
        notificationRepository.deleteByUuid(UUID.fromString(uuid));
    }

    @Cacheable(value = "notifications", key = "#groupName")
    public List<NotificationDTO> getAll(String groupName) {
        GroupEntity group = groupRepository.findByGroupNameIgnoreCase(groupName)
                .orElseThrow(() -> ExceptionFabric.create(GroupNotFoundException.class, ExceptionMessages.GROUP_NOT_FOUND));

        List<NotificationEntity> notificationEntities = notificationRepository.findAllByGroupName(group).stream()
                .filter(notificationEntity -> {
                    if (notificationEntity.getToDate().isBefore(LocalDate.now())) {
                        log.debug("Entity просрочено! {}", notificationEntity.getUuid());
                        toDelete.add(notificationEntity.getUuid());
                        return false;
                    }
                    return true;
                })
                .toList();

        return notificationMapper.notificationEntitiesToNotificationDTOs(notificationEntities);

    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    @CacheEvict(value = "notifications", allEntries = true)
    public void deleteExpired() {
        for (UUID uuid : toDelete) {
            deleteByUUID(uuid.toString());
        }
        toDelete.clear();
    }
}
