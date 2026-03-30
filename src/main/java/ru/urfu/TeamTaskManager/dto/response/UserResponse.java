package ru.urfu.TeamTaskManager.dto.response;

import lombok.Builder;
import lombok.Data;
import ru.urfu.TeamTaskManager.enums.Role;

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
