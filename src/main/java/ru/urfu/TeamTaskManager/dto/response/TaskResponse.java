package ru.urfu.TeamTaskManager.dto.response;

import lombok.Builder;
import lombok.Data;
import ru.urfu.TeamTaskManager.enums.TaskStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private Long creatorId;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private LocalDateTime created_at;
    private TaskStatus taskStatus;
    private UserResponse assignedUser;
}
