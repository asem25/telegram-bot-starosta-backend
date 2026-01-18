package ru.semavin.telegrambot.services.groups;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.semavin.telegrambot.models.GroupEntity;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupParserService {
    private static final String SCHEDULE_URL = "https://public.mai.ru/schedule/data/groups.json";
    private final RestTemplate restTemplate;

    public List<GroupEntity> findAllGroups() {
        JsonArray jsonListOfAllGroups = JsonParser.parseString(getJsonOfListGroups()).getAsJsonArray();
        List<GroupEntity> groups = new ArrayList<>();

        jsonListOfAllGroups.forEach(jsonElement -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String fac = jsonObject.get("fac").getAsString();
            if (fac.equalsIgnoreCase("Институт №3")
                    || fac.equalsIgnoreCase("Институт №9")
                    || fac.equalsIgnoreCase("Институт №7")) {
                groups.add(GroupEntity.builder()
                        .groupName(jsonObject.get("name").getAsString())
                        .build());
            }
        });
        log.info("Найдено {} групп", groups.size());
        return groups;
    }

    private String getJsonOfListGroups() {
        log.info("Получение всех групп");
        return restTemplate.getForObject(SCHEDULE_URL, String.class);
    }
}
