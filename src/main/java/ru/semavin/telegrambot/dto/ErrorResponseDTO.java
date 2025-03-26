package ru.semavin.telegrambot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "DTO для ответа ошибки")
public class ErrorResponseDTO {
    @Schema(description = "Код ошибки", example = "404")
    private String error;
    @Schema(description = "Описания ощибки", example = "Расписания не найдено")
    private String error_description;
}
