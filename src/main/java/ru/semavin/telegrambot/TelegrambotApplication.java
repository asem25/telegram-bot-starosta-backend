package ru.semavin.telegrambot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.services.ScheduleParserService;

import java.io.IOException;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class TelegrambotApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelegrambotApplication.class, args);
	}

}
