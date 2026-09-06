package ru.semavin.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.semavin.telegrambot.dto.DeadlineDTO;
import ru.semavin.telegrambot.models.DeadlineEntity;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.UserEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeadlineMapper {
    @Mapping(target = "groupName", source = "group.groupName")
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "receiverIds", expression = "java(getUserIds(entity.getGroup()))")
    DeadlineDTO toDto(DeadlineEntity entity);

    @Mapping(target = "group", ignore = true)
    @Mapping(target = "creator", ignore = true)
    DeadlineEntity toEntity(DeadlineDTO dto);

    List<DeadlineDTO> toDtoList(List<DeadlineEntity> entities);

    default List<Long> getUserIds(GroupEntity group) {
        if (group == null || group.getUsers() == null) return List.of();
        return group.getUsers().stream().map(UserEntity::getId).toList();
    }
}
