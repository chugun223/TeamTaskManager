package ru.urfu.TeamTaskManager.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserRequest {
    private String username;
    private String email;
    private String password;
}
