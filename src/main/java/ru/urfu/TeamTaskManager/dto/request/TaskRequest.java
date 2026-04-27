package ru.urfu.TeamTaskManager.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskRequest {
    @NotNull(message = "Task title cannot be null")
    @NotBlank(message = "Task title cannot be empty")
    @Size(min = 1, max = 40, message = "Task title must be 1-40 elements")
    private String title;

    @Size(max = 500, message = "Team's description must be 500 elements maximum")
    private String description;

    @Future(message = "Deadline must be later than the current time")
    private LocalDateTime deadline;

    private Long assignedUserId;
}