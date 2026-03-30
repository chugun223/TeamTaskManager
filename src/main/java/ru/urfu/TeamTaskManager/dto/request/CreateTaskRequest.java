import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateTaskRequest {
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Long assignedUserId;
}