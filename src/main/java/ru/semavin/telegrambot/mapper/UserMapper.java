package ru.semavin.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.models.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    /**
     * Public Mini App representation deliberately contains no direct identity.
     */
    @Mapping(target = "groupName", source = "group.groupName")
    UserDTO userToUserDTO(UserEntity user);
}
