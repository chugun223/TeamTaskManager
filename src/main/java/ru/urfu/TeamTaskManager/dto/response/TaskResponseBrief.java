package ru.urfu.TeamTaskManager.dto.response;

import lombok.Builder;
import lombok.Data;
import ru.urfu.TeamTaskManager.enums.TaskStatus;

@Data
@Builder
public class TaskResponseBrief {
    private Long id;
    private String title;
    private TaskStatus taskStatus;
}
