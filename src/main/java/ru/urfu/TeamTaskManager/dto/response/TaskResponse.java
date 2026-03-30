package TeamManagerTest.ru.urfu.dto.response;

import TeamManagerTest.ru.urfu.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private TaskStatus taskStatus;
    private UserResponseBrief assignedUser;
}
