package ru.semavin.telegrambot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadlineDTO {
    private UUID uuid;
    private String title;
    private String description;
    private LocalDate dueDate;
    private String groupName;
    private Long creatorId;
    private List<Long> receiverIds;

    private boolean notified3Days;
    private boolean notified1Day;
}
