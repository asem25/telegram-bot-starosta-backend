package ru.semavin.telegrambot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для пользователя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для пользователя в системе")
public class UserDTO {

    @Builder.Default
    @Schema(description = "Роль пользователя", example = "STUDENT")
    private String role = "STUDENT";


    @Size(min = 3, max = 16, message = "Название группы должно быть от 3 до 16 символов")
    @Schema(description = "Название группы", example = "М3О-303С-22")
    private String groupName;
}
