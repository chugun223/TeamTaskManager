package ru.urfu.TeamTaskManager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamRequest {
    @NotNull(message = "Team's name cannot be null")
    @NotBlank(message = "Team's name cannot be empty")
    @Size(min = 5, max = 50, message = "Team's name must be 5-50 elements")
    private String name;

    @Size(max = 500, message = "Team's description must be 500 elements maximum")
    private String description;
}
