package ru.urfu.TeamTaskManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.urfu.TeamTaskManager.domain.Task;
import ru.urfu.TeamTaskManager.domain.User;
import ru.urfu.TeamTaskManager.repository.TaskRepository;
import ru.urfu.TeamTaskManager.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public void sendTaskAssignedNotification(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        log.info("Preparing to send task assignment notification for taskId={} to userId={}", taskId, userId);
        String subject = "Новая задача: " + task.getTitle();
        String body = String.format(
                "Здравствуйте, %s!\n\nВам назначена новая задача:\n" + "Название: %s\nОписание: %s\nДедлайн: %s\n\nС прискорбием,\nTeamTaskManager",
                user.getUsername(),
                task.getTitle(),
                task.getDescription() != null ? task.getDescription() : "-",
                task.getDeadline() != null ? task.getDeadline() : "-"
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("team.task.manager.project@gmail.com");
        log.info("Sending task assignment notification email to {} for taskId={}", user.getEmail(), taskId);
        mailSender.send(message);
    }
}
