package ru.semavin.telegrambot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDTO {

    @Size(min = 3, max = 16, message = "Название группы должно быть от 3 до 16 символов")
    @Schema(description = "Название группы", example = "М3О-303С-22")
    private String groupName;
    @Size(min = 3, max = 50, message = "Тэг должно быть от 3 до 50 символов")
    @Schema(description = "Тэг пользователя в телеграмм", example = "ivan23424")
    private String starosta_username;
    @Schema(description = "Список студентов группы", example = "[ivan23424, anton234124]")
    private List<String> names_students;
}
