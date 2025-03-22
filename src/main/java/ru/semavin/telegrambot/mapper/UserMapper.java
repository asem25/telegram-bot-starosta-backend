package ru.semavin.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.models.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    /**
     * Из DTO -> Entity: игнорируем поле group,
     * т.к. будем ставить его вручную в сервисном слое.
     */
    @Mapping(target = "group", ignore = true)
    UserEntity userDTOToUser(UserDTO userDTO);

    /**
     * Из Entity -> DTO: если group != null,
     * мапим group.groupName -> dto.groupName.
     */
    @Mapping(target = "groupName", source = "group.groupName")
    UserDTO userToUserDTO(UserEntity user);
}
