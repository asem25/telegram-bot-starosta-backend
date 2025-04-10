package ru.semavin.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.models.ScheduleEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {
    @Mapping(target = "group.groupName", source = "groupName")
    ScheduleEntity toScheduleEntity(ScheduleDTO dto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "groupName", source = "group.groupName")
    @Mapping(target = "teacherName", expression = "java(entity.getTeacher().getFirstName() + \" \" + entity.getTeacher().getPatronymic() + \" \" + entity.getTeacher().getLastName())")
    ScheduleDTO toScheduleDTO(ScheduleEntity entity);

    List<ScheduleDTO> toScheduleDTOList(List<ScheduleEntity> entities);
    List<ScheduleEntity> toScheduleEntityList(List<ScheduleDTO> dtos);
}
