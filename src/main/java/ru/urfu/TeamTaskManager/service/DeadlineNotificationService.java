package ru.urfu.TeamTaskManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.TeamTaskManager.domain.Task;
import ru.urfu.TeamTaskManager.enums.TaskStatus;
import ru.urfu.TeamTaskManager.repository.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineNotificationService {

    private final TaskRepository taskRepository;
    private final MailService mailService;

    @Scheduled(cron = "0 0 */4 * * *")
    @Transactional
    public void checkDeadlinesAndNotify() {
        log.info("Starting daily deadline check");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        log.info("Checking deadlines between {} and {}", now, tomorrow);

        List<Task> tasks = taskRepository.findByDeadlineBetweenAndTaskStatusNot(now, tomorrow);

        log.info("Found {} tasks with deadline approaching", tasks.size());

        for (Task task : tasks) {
            if (task.getAssignedUser() != null) {
                mailService.sendDeadlineReminder(task.getId(), task.getAssignedUser().getId());
                task.setDeadlineNotified(true);
                log.info("Sent reminder for task {} to user {}", task.getId(), task.getAssignedUser().getId());
            } else {
                log.warn("Task {} has no assigned user, skipping", task.getId());
            }
        }
    }
}