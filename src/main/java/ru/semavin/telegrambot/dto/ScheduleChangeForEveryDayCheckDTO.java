package ru.semavin.telegrambot.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ScheduleChangeForEveryDayCheckDTO {
    List<ScheduleChangeForFrontDTO> scheduleChangeEntityList;
}
