package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.mapper.UserMapper;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.repositories.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private Set<String> setGroups = new HashSet<>();
    @Transactional
    public String save(UserDTO user){
        log.info("Saving user: {}", user);

        user.setRole("STUDENT");

        UserEntity fromRepository = userRepository.save(userMapper.userDTOToUser(user));
        log.info("User saved: {}", fromRepository);
        setGroups = getAllGroups();
        return fromRepository.getUsername();
    }
    public Set<String> getAllGroups(){
        return setGroups;
    }
}
