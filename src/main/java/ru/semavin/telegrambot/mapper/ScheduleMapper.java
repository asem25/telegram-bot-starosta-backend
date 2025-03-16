package ru.semavin.telegrambot.mapper;

import org.mapstruct.Mapper;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.models.ScheduleEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {
    ScheduleEntity toScheduleEntity(ScheduleDTO dto);
    ScheduleDTO toScheduleDTO(ScheduleEntity entity);

    List<ScheduleDTO> toScheduleDTOList(List<ScheduleEntity> entities);
    List<ScheduleEntity> toScheduleEntityList(List<ScheduleDTO> dtos);
}
