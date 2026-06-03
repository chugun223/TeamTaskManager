package ru.urfu.TeamTaskManager.eventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.urfu.TeamTaskManager.event.TaskDeletedEvent;
import ru.urfu.TeamTaskManager.service.MailService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDeletedListener {

    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void taskAssigned(TaskDeletedEvent event) {
        log.info("Received TaskDeletedEvent for taskId={} assigned to userId={}", event.taskId(), event.assignedUserId());
        mailService.sendTaskDeletedNotification(event.taskId(), event.taskTitle(), event.assignedUserId());
    }
}
