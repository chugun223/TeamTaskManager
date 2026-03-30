package TeamManagerTest.ru.urfu.dto.response;

import TeamManagerTest.ru.urfu.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseBrief {
    private Long id;
    private String username;
    private String email;
    private Role role;
}
