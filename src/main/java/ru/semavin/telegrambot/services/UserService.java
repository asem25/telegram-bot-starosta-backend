package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.mapper.UserMapper;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.repositories.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    public String save(UserDTO user){
        log.info("Saving user: {}", user);

        user.setRole("STUDENT");

        UserEntity fromRepository = userRepository.save(userMapper.userDTOToUser(user));
        log.info("User saved: {}", fromRepository);
        return fromRepository.getUsername();
    }
}
