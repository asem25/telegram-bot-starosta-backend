package ru.semavin.telegrambot.mapper;

import org.apache.catalina.User;
import org.mapstruct.Mapper;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.models.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO userToUserDTO(User user);
    UserEntity userDTOToUser(UserDTO userDTO);
}
