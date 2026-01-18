package ru.semavin.telegrambot.models;

import ru.semavin.telegrambot.dto.ScheduleDTO;

import java.util.List;

public record ScheduleGroupChunk(
        String groupName,
        List<ScheduleDTO> schedule
) {
}
