package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleParserService scheduleParserService;
    private final ScheduleMapper scheduleMapper;
    private final SemesterService semesterService;

    public List<ScheduleDTO> getScheduleFromDataBase(String groupName, String week){
        List<ScheduleDTO> scheduleDTOS;
        if (week != null) {
            scheduleDTOS = scheduleMapper.toScheduleDTOList(scheduleRepository.findAllByGroupNameIgnoreCaseAndLessonWeek(groupName, Integer.parseInt(week)));
        }else{
            String currentWeek = semesterService.getCurrentWeek();
            scheduleDTOS =  scheduleMapper.toScheduleDTOList(scheduleRepository.findAllByGroupNameIgnoreCaseAndLessonWeek(groupName, Integer.parseInt(currentWeek)));
        }
        if (scheduleDTOS.isEmpty()){
            return getActualSchedule(groupName, week);
        }
        return scheduleDTOS;
    }
    public List<ScheduleDTO> getActualSchedule(String groupName, String week){
        List<ScheduleEntity> scheduleEntities;
        try {
            scheduleEntities = scheduleRepository.saveAll(scheduleParserService.findScheduleByGroup(groupName, week));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return scheduleMapper.toScheduleDTOList(scheduleEntities);
    }
}
