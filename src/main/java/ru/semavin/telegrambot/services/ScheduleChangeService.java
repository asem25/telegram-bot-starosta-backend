package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.ScheduleChangeDTO;
import ru.semavin.telegrambot.dto.ScheduleChangeForEveryDayCheckDTO;
import ru.semavin.telegrambot.dto.ScheduleChangeForFrontDTO;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.repositories.ScheduleChangeRepository;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ScheduleChangeService {
    private final ScheduleChangeRepository changeRepository;
    private final ScheduleRepository scheduleRepository;
    private final GroupService groupService;

    @CacheEvict(value = "scheduleDay", key = "#groupName + '-' + #dto.oldLessonDate.format(T(ru.semavin.telegrambot.utils.DateUtils).FORMATTER)")
    public ScheduleChangeEntity createOrUpdate(ScheduleChangeDTO dto, String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);
        ScheduleEntity scheduleEntity = scheduleRepository.findSchedule(
                group, dto.getOldLessonDate(), dto.getOldStartTime(),
                dto.getOldEndTime(), LessonType.valueOfString(dto.getLessonType()), dto.getSubjectName()
        );
        ScheduleChangeEntity entity = new ScheduleChangeEntity();

        entity.setGroup(group);
        entity.setSubjectName(dto.getSubjectName());
        entity.setLessonType(dto.getLessonType());
        entity.setTeacherName(dto.getTeacherName());
        entity.setClassroom(dto.getClassroom());

        entity.setOldLessonDate(dto.getOldLessonDate());
        entity.setOldStartTime(dto.getOldStartTime());
        entity.setOldEndTime(dto.getOldEndTime());
        entity.setNewStartTime(dto.getNewStartTime());
        entity.setNewEndTime(dto.getNewEndTime());
        entity.setNewLessonDate(dto.getNewLessonDate());
        entity.setOldControlSum(scheduleEntity.getControlSum());
        entity.setDescription(dto.getDescription());
        entity.setDeleted(false);

        return changeRepository.save(entity);
    }

    public ScheduleChangeForEveryDayCheckDTO getChangesDtoForDay(String groupName, LocalDate date) {
        GroupEntity group = groupService.findEntityByName(groupName);
        return changesToDto(changeRepository.findAllByGroupAndOldLessonDate(group, date));
    }

    public List<ScheduleChangeEntity> getChangesDtoAnyDay(String groupName, LocalDate date) {
        GroupEntity group = groupService.findEntityByName(groupName);
        return changeRepository.findAllByGroupAndDate(group, date);
    }

    private ScheduleChangeForEveryDayCheckDTO changesToDto(List<ScheduleChangeEntity> scheduleChangeEntities) {
        return ScheduleChangeForEveryDayCheckDTO.builder()
                .scheduleChangeEntityList(scheduleChangeEntities
                        .stream()
                        .map(this::prepareForFront)
                        .toList())
                .build();
    }

    private ScheduleChangeForFrontDTO prepareForFront(ScheduleChangeEntity scheduleChangeEntity) {
        return ScheduleChangeForFrontDTO.builder()
                .deleted(scheduleChangeEntity.isDeleted())
                .classroom(scheduleChangeEntity.getClassroom())
                .lessonType(scheduleChangeEntity.getLessonType())
                .newEndTime(scheduleChangeEntity.getNewEndTime())
                .newLessonDate(scheduleChangeEntity.getNewLessonDate())
                .newStartTime(scheduleChangeEntity.getNewStartTime())
                .oldEndTime(scheduleChangeEntity.getOldEndTime())
                .oldLessonDate(scheduleChangeEntity.getOldLessonDate())
                .oldStartTime(scheduleChangeEntity.getOldStartTime())
                .subjectName(scheduleChangeEntity.getSubjectName())
                .teacherName(scheduleChangeEntity.getTeacherName())
                .description(scheduleChangeEntity.getDescription())
                .build();
    }

    @CacheEvict(value = "scheduleDay", key = "#groupName + '-' + #dto.oldLessonDate.format(T(ru.semavin.telegrambot.utils.DateUtils).FORMATTER)")
    public void markAsDeleted(ScheduleChangeDTO dto, String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);
        ScheduleEntity scheduleEntity = scheduleRepository.findSchedule(
                group, dto.getOldLessonDate(), dto.getOldStartTime(),
                dto.getOldEndTime(), LessonType.valueOfString(dto.getLessonType()), dto.getSubjectName()
        );
        ScheduleChangeEntity entity = new ScheduleChangeEntity();

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
        entity.setOldControlSum(scheduleEntity.getControlSum());
        entity.setDescription(dto.getDescription());
        entity.setDeleted(true);

        changeRepository.save(entity);
    }

}
