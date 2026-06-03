package ru.urfu.TeamTaskManager.event;

public record TaskDeletedEvent(Long taskId, String taskTitle, Long assignedUserId) {
}
