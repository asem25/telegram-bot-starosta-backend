package ru.semavin.telegrambot.services.schedules;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemesterServiceConfigurationTest {
    @Test
    void acceptsIsoEnvironmentDates() {
        SemesterService service = new SemesterService("2026-09-01", "2027-01-31");

        assertEquals(LocalDate.of(2026, 9, 1), service.getStartSemester());
        assertEquals(LocalDate.of(2027, 1, 31), service.getEndSemester());
    }

    @Test
    void retainsLegacyDateFormatCompatibility() {
        SemesterService service = new SemesterService("01.09.2026", "31.01.2027");

        assertEquals(LocalDate.of(2026, 9, 1), service.getStartSemester());
    }

    @Test
    void rejectsReversedSemesterBounds() {
        assertThrows(IllegalStateException.class,
                () -> new SemesterService("2027-01-31", "2026-09-01"));
    }
}
