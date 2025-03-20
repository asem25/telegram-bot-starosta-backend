package ru.semavin.telegrambot.services.ping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PingDailyBotService {

    private final RestTemplate restTemplate;
    @Value("${url.bot}")
    private String botUrl;

    @Scheduled(fixedRate = 60000 * 3)
    public void pingDailyBot() {
        log.info("Telegram bot ping starting");
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(botUrl + "/", String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Telegram bot ping finished success: {}", response.getBody());
            }else{
                log.error("Telegram bot ping failed: {}", response.getBody());
            }

        } catch (RestClientException e) {
            log.warn("Telegram Bot service error");
        }
    }
}
