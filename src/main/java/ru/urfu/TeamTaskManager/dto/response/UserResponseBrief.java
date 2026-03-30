package ru.urfu.TeamTaskManager.dto.response;

import lombok.Builder;
import lombok.Data;
import ru.urfu.TeamTaskManager.enums.Role;

@Data
@Builder
public class UserResponseBrief {
    private Long id;
    private String username;
    private String email;
    private Role role;
}
