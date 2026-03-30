package TeamManagerTest.ru.urfu.dto.response;

import TeamManagerTest.ru.urfu.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private TeamResponseBrief team;
    private List<TaskResponseBrief> tasks;
}
