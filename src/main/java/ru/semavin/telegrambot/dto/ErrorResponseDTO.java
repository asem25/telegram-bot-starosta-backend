package ru.semavin.telegrambot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponseDTO {
    private String error;
    private String error_description;
}
