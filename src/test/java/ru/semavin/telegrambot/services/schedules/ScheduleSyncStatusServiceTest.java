package ru.semavin.telegrambot.services.schedules;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleSyncStatusServiceTest {

    @Test
    void storesLastSuccessfulSyncInMemoryByGroup() {
        Instant syncTime = Instant.parse("2026-09-06T11:30:00Z");
        ScheduleSyncStatusService service =
                new ScheduleSyncStatusService(Clock.fixed(syncTime, ZoneOffset.UTC));

        service.markSuccessfulSync("GROUP-1");

        assertThat(service.getLastSuccessfulSync("GROUP-1")).contains(syncTime);
        assertThat(service.getLastSuccessfulSync("GROUP-2")).isEmpty();
    }
}
