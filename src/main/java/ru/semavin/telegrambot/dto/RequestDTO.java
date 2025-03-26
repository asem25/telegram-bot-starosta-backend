package ru.semavin.telegrambot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDTO {
    private Long id;
    private String telegramTagUser;
    private String groupName;
}
