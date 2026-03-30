package TeamManagerTest.ru.urfu.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamResponse {
    private Long id;
    private String name;
    private String description;
    private List<UserResponseBrief> members;
}

