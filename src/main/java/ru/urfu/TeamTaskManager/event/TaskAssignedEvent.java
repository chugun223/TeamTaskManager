package ru.urfu.TeamTaskManager.event;

public record TaskAssignedEvent(Long taskId, Long assignedUserId) {
}