package ru.semavin.telegrambot.services.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.ScheduleDTO;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerCalendarISCService {

    public static final String BEGIN_VEVENT = "BEGIN:VEVENT";
    public static final String DTSTART_TZID = "DTSTART;TZID=";
    public static final String DTEND_TZID = "DTEND;TZID=";
    public static final String UID = "UID:";
    public static final String SUMMARY = "SUMMARY:";
    public static final String LOCATION = "LOCATION:";
    public static final String DESCRIPTION = "DESCRIPTION:";
    public static final String END_VEVENT = "END:VEVENT";
    public static final String END_VCALENDAR = "END:VCALENDAR";
    private static final String VCALENDAR = "BEGIN:VCALENDAR";
    private static final String VERSION = "VERSION:2.0";
    private static final String PRODID = "PRODID:-//TelegramBot-Starosta//SemesterSchedule//RU";
    private static final String CALSCALE = "CALSCALE:GREGORIAN";
    private static final String CALNAME = "X-WR-CALNAME:";
    private static final String telegram_tag = "@telegrambot-starosta";
    public static final String CRLF = "\r\n";

    private final SemesterService semesterService;
    private final ScheduleService scheduleService;


    private static final DateTimeFormatter ICS_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public String getIscCalendarByGroupName(String groupName) {
        val ics = buildCalendarISC(groupName);
        log.debug("Сформирован .ics за семестр для группы {}. Длина файла: {} символов",
                groupName, ics.length());
        return ics;
    }

    private String buildCalendarISC(
            String groupName
    ) {
        ZoneId zoneId = ZoneId.of("Europe/Moscow");
        val schDtosList = scheduleService.getScheduleForISC(groupName);

        StringBuilder sb = new StringBuilder();

        sb.append(VCALENDAR).append(CRLF);
        sb.append(VERSION).append(CRLF);
        sb.append(PRODID).append(CRLF);
        sb.append(CALSCALE).append(CRLF);
        sb.append(CALNAME).append(escapeText("Расписание " + groupName + " (семестр)")).append(CRLF);

        schDtosList.forEach(dto ->
                processBuildForSchedule(groupName, dto, zoneId, sb)
        );

        sb.append(END_VCALENDAR).append(CRLF);

        return sb.toString();
    }

    private void processBuildForSchedule(String groupName, ScheduleDTO dto, ZoneId zoneId, StringBuilder sb) {
        val start = LocalDateTime.of(dto.getLessonDate(), dto.getStartTime());
        val end = LocalDateTime.of(dto.getLessonDate(), dto.getEndTime());

        val dtStart = start.atZone(zoneId).format(ICS_DATE_TIME_FORMATTER);
        val dtEnd = end.atZone(zoneId).format(ICS_DATE_TIME_FORMATTER);

        val uid = dto.getControlSum();

        String summary = dto.getSubjectName();
        if (dto.getLessonType() != null && !dto.getLessonType().isBlank()) {
            summary += " (" + getStringType(dto) + ")";
        }

        val description = new StringBuilder("Группа: " + groupName);
        if (dto.getTeacherName() != null && !dto.getTeacherName().isBlank()) {
            description.append("\nПреподаватель: ").append(dto.getTeacherName());
            if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
                description.append("\nКомментарий: ").append(dto.getDescription());
            }
        }

        sb.append(BEGIN_VEVENT).append(CRLF);
        sb.append(DTSTART_TZID).append(zoneId).append(":").append(dtStart).append(CRLF);
        sb.append(DTEND_TZID).append(zoneId).append(":").append(dtEnd).append(CRLF);
        sb.append(UID).append(uid).append(CRLF);
        sb.append(SUMMARY).append(escapeText(summary)).append(CRLF);

        if (dto.getClassroom() != null && !dto.getClassroom().isBlank()) {
            sb.append(LOCATION).append(escapeText(dto.getClassroom())).append(CRLF);
        }

        sb.append(DESCRIPTION).append(escapeText(description.toString())).append(CRLF);
        sb.append(END_VEVENT).append(CRLF);
    }

    private String getStringType(ScheduleDTO dto) {
        return switch (dto.getLessonType()) {
            case "PRACTICAL" -> "ПЗ";
            case "LECTURE" -> "ЛК";
            case "LAB" -> "ЛР";
            case "EXAM" -> "ЭКЗ";
            default -> "Не определено";
        };
    }

    private String escapeText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace(CRLF, "\\n")
                .replace("\n", "\\n");
    }

}
