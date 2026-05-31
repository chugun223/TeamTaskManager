package ru.urfu.TeamTaskManager.eventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.urfu.TeamTaskManager.event.TaskAssignedEvent;
import ru.urfu.TeamTaskManager.service.MailService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAssignedListener {

    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void taskAssigned(TaskAssignedEvent event) {
        log.info("Received TaskAssignedEvent for taskId={} assigned to userId={}", event.taskId(), event.assignedUserId());
        mailService.sendTaskAssignedNotification(event.taskId(), event.assignedUserId());
    }
}
