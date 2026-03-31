package ru.urfu.TeamTaskManager.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.urfu.TeamTaskManager.dto.mapper.DtoMapper;
import ru.urfu.TeamTaskManager.dto.request.TeamRequest;
import ru.urfu.TeamTaskManager.dto.response.TeamResponse;
import ru.urfu.TeamTaskManager.dto.response.TeamResponseBrief;
import ru.urfu.TeamTaskManager.dto.response.UserResponseBrief;
import ru.urfu.TeamTaskManager.service.TeamService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final DtoMapper dtoMapper;

    @PostMapping
    public TeamResponse createTeam(@RequestParam Long userId, @RequestBody TeamRequest request) {
        var team = teamService.createTeam(userId, request);
        return dtoMapper.toTeamResponse(team);
    }

    @GetMapping
    public List<TeamResponseBrief> getAllTeams() {
        return teamService.getAllTeams().stream()
                .map(dtoMapper::toTeamResponseBrief)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public TeamResponse getTeamById(@PathVariable Long id) {
        var team = teamService.getTeamById(id);
        return dtoMapper.toTeamResponse(team);
    }

    @DeleteMapping("/{teamId}")
    public void deleteTeam(@PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
    }

    @GetMapping("/{teamId}/members")
    public List<UserResponseBrief> getTeamUsers(@PathVariable Long teamId) {
        return teamService.getTeamMembers(teamId).stream()
                .map(dtoMapper::toUserResponseBrief)
                .collect(Collectors.toList());
    }

    @PutMapping("/{teamId}")
    public TeamResponse updateTeam(@PathVariable Long teamId, @RequestBody TeamRequest request) {
        var team = teamService.updateTeam(teamId, request);
        return dtoMapper.toTeamResponse(team);
    }
}
