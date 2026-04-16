package ru.urfu.TeamTaskManager.dto.mapper;

import org.springframework.stereotype.Component;
import ru.urfu.TeamTaskManager.domain.*;
import ru.urfu.TeamTaskManager.dto.response.*;

import java.util.stream.Collectors;

@Component
public class DtoMapper {
    public UserResponse toUserResponse(User user) {
        if (user == null) return null;

        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole());

        if (user.getTeam() != null) {
            builder.team(toTeamResponseBrief(user.getTeam()));
        }

        if (user.getTasks() != null && !user.getTasks().isEmpty()) {
            builder.tasks(user.getTasks().stream()
                    .map(this::toTaskResponseBrief)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public UserResponse toUserResponseBrief(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .team(null)
                .tasks(null)
                .build();
    }

    public TeamResponse toTeamResponse(Team team) {
        if (team == null) return null;

        TeamResponse.TeamResponseBuilder builder = TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription());

        if (team.getMembers() != null && !team.getMembers().isEmpty()) {
            builder.members(team.getMembers().stream()
                    .map(this::toUserResponseBrief)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public TeamResponse toTeamResponseBrief(Team team) {
        if (team == null) return null;
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .members(null)
                .build();
    }

    public TaskResponse toTaskResponse(Task task) {
        if (task == null) return null;

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .created_at(task.getCreated_at())
                .deadline(task.getDeadline())
                .taskStatus(task.getTaskStatus())
                .assignedUser(toUserResponseBrief(task.getAssignedUser()))
                .build();
    }

    public TaskResponse toTaskResponseBrief(Task task) {
        if (task == null) return null;
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .created_at(task.getCreated_at())
                .deadline(task.getDeadline())
                .taskStatus(task.getTaskStatus())
                .assignedUser(null)
                .build();
    }
}
