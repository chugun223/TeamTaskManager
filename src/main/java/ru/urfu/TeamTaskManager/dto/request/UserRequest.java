package ru.urfu.TeamTaskManager.dto.request;

import lombok.*;

@Data
@Builder
public class UserRequest {
    private String username;
    private String email;
    private String password;
}
