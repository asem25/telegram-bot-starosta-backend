package ru.semavin.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.semavin.telegrambot.dto.GroupDTO;
import ru.semavin.telegrambot.models.GroupEntity;
@Mapper(componentModel = "spring")
public interface GroupMapper {

    /**
     * Преобразуем GroupEntity -> GroupDTO.
     * Берём group.starosta.username и складываем в dto.starostaUsername.
     */
    @Mapping(target = "starosta_username", source = "starosta.username")
    GroupDTO groupToDTO(GroupEntity group);

    /**
     * Преобразуем GroupDTO -> GroupEntity.
     * Игнорируем поле starosta – чтобы избежать лишней логики в маппере.
     * Назначение реального старосты (UserEntity) делаем в сервисе,
     * если нужно.
     */
    @Mapping(target = "starosta", ignore = true)
    GroupEntity groupDTOToGroup(GroupDTO dto);
}

