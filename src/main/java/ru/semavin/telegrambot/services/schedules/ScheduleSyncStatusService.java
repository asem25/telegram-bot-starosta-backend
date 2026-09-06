package ru.semavin.telegrambot.services.schedules;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ScheduleSyncStatusService {

    private final ConcurrentMap<String, Instant> lastSuccessfulSyncByGroup = new ConcurrentHashMap<>();
    private final Clock clock;

    public ScheduleSyncStatusService() {
        this(Clock.systemUTC());
    }

    ScheduleSyncStatusService(Clock clock) {
        this.clock = clock;
    }

    public void markSuccessfulSync(String groupName) {
        lastSuccessfulSyncByGroup.put(groupName, clock.instant());
    }

    public Optional<Instant> getLastSuccessfulSync(String groupName) {
        return Optional.ofNullable(lastSuccessfulSyncByGroup.get(groupName));
    }
}
