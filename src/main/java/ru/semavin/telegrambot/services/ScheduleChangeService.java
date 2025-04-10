package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.ScheduleChangeDTO;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.repositories.ScheduleChangeRepository;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ScheduleChangeService {
    private final ScheduleChangeRepository changeRepository;
    private final GroupService groupService;

    @CacheEvict(value = "scheduleDay", key = "#groupName + '-' + #dto.oldLessonDate.format(T(ru.semavin.telegrambot.utils.DateUtils).FORMATTER)")
    public ScheduleChangeEntity createOrUpdate(ScheduleChangeDTO dto, String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);
        ScheduleChangeEntity entity = changeRepository.findByGroupAndOldLessonDateAndOldStartTime(group, dto.getOldLessonDate(), dto.getOldStartTime())
                .orElse(new ScheduleChangeEntity());

        entity.setGroup(group);
        entity.setSubjectName(dto.getSubjectName());
        entity.setLessonType(dto.getLessonType());
        entity.setTeacherName(dto.getTeacherName());
        entity.setClassroom(dto.getClassroom());

        entity.setOldLessonDate(dto.getOldLessonDate());
        entity.setOldStartTime(dto.getOldStartTime());
        entity.setOldEndTime(dto.getOldEndTime());

        entity.setNewLessonDate(dto.getNewLessonDate());
        entity.setNewStartTime(dto.getNewStartTime());
        entity.setNewEndTime(dto.getNewEndTime());

        entity.setDescription(dto.getDescription());
        entity.setDeleted(false);

        return changeRepository.save(entity);
    }

    public List<ScheduleChangeEntity> getChangesForDay(String groupName, LocalDate date) {
        GroupEntity group = groupService.findEntityByName(groupName);
        return changeRepository.findAllByGroupAndOldLessonDate(group, date);
    }

    @CacheEvict(value = "scheduleDay", key = "#groupName + '-' + #dto.oldLessonDate.format(T(ru.semavin.telegrambot.utils.DateUtils).FORMATTER)")
    public void markAsDeleted(ScheduleChangeDTO dto, String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);
        ScheduleChangeEntity entity = changeRepository.findByGroupAndOldLessonDateAndOldStartTime(group, dto.getOldLessonDate(), dto.getOldStartTime())
                .orElse(new ScheduleChangeEntity());

        entity.setGroup(group);
        entity.setSubjectName(dto.getSubjectName());
        entity.setLessonType(dto.getLessonType());
        entity.setTeacherName(dto.getTeacherName());
        entity.setClassroom(dto.getClassroom());

        entity.setOldLessonDate(dto.getOldLessonDate());
        entity.setOldStartTime(dto.getOldStartTime());
        entity.setOldEndTime(dto.getOldEndTime());

        entity.setNewLessonDate(dto.getNewLessonDate());
        entity.setNewStartTime(dto.getNewStartTime());
        entity.setNewEndTime(dto.getNewEndTime());

        entity.setDescription(dto.getDescription());
        entity.setDeleted(true);

        changeRepository.save(entity);
    }

}
