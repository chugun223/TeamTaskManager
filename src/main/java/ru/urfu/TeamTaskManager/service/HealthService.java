package ru.urfu.TeamTaskManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private final HealthEndpoint healthEndpoint;
    private final MailService mailService;

    private boolean wasDown = false;

    @Scheduled(fixedDelay = 600000)
    public void checkSystemHealth() {
        HealthComponent health = healthEndpoint.health();
        boolean isUp = health.getStatus().getCode().equals("UP");

        if (!isUp) {
            log.warn("Health check failed");
            if (!wasDown) {
                mailService.sendHealthAlert(
                        "Системное предупреждение: Проверка работоспособности не пройдена.",
                        "Система неисправна.\nВремя: " + java.time.LocalDateTime.now()
                );
                wasDown = true;
            }
        }
        else {
            if (wasDown) {
                mailService.sendHealthAlert(
                        "Работа системы восстановлена",
                        "Система вновь работает.\nВремя: " + java.time.LocalDateTime.now()
                );
                wasDown = false;
                log.info("Health check is ok again, recovery email sent");
            }
            else {
                log.info("Health check is passed");
            }
        }
    }
}
