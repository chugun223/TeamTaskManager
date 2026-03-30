package TeamManagerTest.ru.urfu.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTeamRequest {
    private String name;
    private String description;
}
