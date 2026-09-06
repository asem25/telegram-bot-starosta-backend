package ru.semavin.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.semavin.telegrambot.dto.GroupDTO;
import ru.semavin.telegrambot.models.GroupEntity;
@Mapper(componentModel = "spring")
public interface GroupMapper {

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
