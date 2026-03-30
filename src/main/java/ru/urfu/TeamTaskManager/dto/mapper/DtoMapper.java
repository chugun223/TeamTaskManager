package TeamManagerTest.ru.urfu.dto.mapper;

import TeamManagerTest.ru.urfu.domain.*;
import TeamManagerTest.ru.urfu.dto.response.*;
import org.springframework.stereotype.Component;

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

    public UserResponseBrief toUserResponseBrief(User user) {
        if (user == null) return null;
        return UserResponseBrief.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
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

    public TeamResponseBrief toTeamResponseBrief(Team team) {
        if (team == null) return null;
        return TeamResponseBrief.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .build();
    }

    public TaskResponse toTaskResponse(Task task) {
        if (task == null) return null;

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .deadline(task.getDeadline())
                .taskStatus(task.getTaskStatus())
                .assignedUser(toUserResponseBrief(task.getAssignedUser()))
                .build();
    }

    public TaskResponseBrief toTaskResponseBrief(Task task) {
        if (task == null) return null;
        return TaskResponseBrief.builder()
                .id(task.getId())
                .title(task.getTitle())
                .taskStatus(task.getTaskStatus())
                .build();
    }
}
