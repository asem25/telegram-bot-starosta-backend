package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.RequestDTO;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.RequestEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.repositories.GroupRepository;
import ru.semavin.telegrambot.repositories.RequestRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.GroupNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public RequestDTO save(RequestDTO requestDTO) {
        log.info("Пришел запрос на добавление в группу {}, от пользователя {}", requestDTO.getGroupName(), requestDTO.getTelegramTagUser());
        GroupEntity group = groupRepository.findByGroupNameIgnoreCase(requestDTO.getGroupName()).orElseThrow(
                () -> ExceptionFabric.create(GroupNotFoundException.class, ExceptionMessages.GROUP_NOT_FOUND)
        );
        UserEntity userEntity = userRepository.findByUsername(requestDTO.getTelegramTagUser()).orElseThrow(
                () -> ExceptionFabric.create(UserNotFoundException.class, ExceptionMessages.USER_NOT_FOUND)
        );
        requestRepository.save(
                RequestEntity.builder()
                        .group(group)
                        .user(userEntity)
                        .build());
        log.info("Запрос {} сохранен!", requestDTO.getTelegramTagUser());
        return requestDTO;
    }
}
