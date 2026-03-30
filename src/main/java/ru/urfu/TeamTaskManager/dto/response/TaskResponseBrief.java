package TeamManagerTest.ru.urfu.dto.response;

import TeamManagerTest.ru.urfu.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskResponseBrief {
    private Long id;
    private String title;
    private TaskStatus taskStatus;
}
