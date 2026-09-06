package ru.semavin.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.semavin.telegrambot.dto.NotificationDTO;
import ru.semavin.telegrambot.models.NotificationEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "groupName", source = "group.groupName")
    NotificationDTO notificationToNotificationDTO(NotificationEntity notification);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "group", ignore = true)
    NotificationEntity notificationDTOToNotificationEntity(NotificationDTO notificationDTO);

    List<NotificationEntity> notificationDTOsToNotificationEntities(List<NotificationDTO> notificationDTOs);

    List<NotificationDTO> notificationEntitiesToNotificationDTOs(List<NotificationEntity> notificationEntities);
}
