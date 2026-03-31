package ru.urfu.TeamTaskManager.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamRequest {
    private String name;
    private String description;
}
