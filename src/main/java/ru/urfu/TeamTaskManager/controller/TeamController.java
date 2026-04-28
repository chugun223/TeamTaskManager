package ru.urfu.TeamTaskManager.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.urfu.TeamTaskManager.dto.mapper.DtoMapper;
import ru.urfu.TeamTaskManager.dto.request.TeamRequest;
import ru.urfu.TeamTaskManager.dto.response.*;
import ru.urfu.TeamTaskManager.service.TeamService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Validated
public class TeamController {
    private final TeamService teamService;
    private final DtoMapper dtoMapper;

    @PreAuthorize("hasRole('NONE')")
    @PostMapping
    public TeamResponse createTeam(@RequestBody @Valid TeamRequest request) {
        var team = teamService.createTeam(request);
        return dtoMapper.toTeamResponse(team);
    }

    @GetMapping
    public Page<TeamResponse> getAllTeams(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return teamService.getAllTeams(page, size).map(dtoMapper::toTeamResponseBrief);
    }

    @GetMapping("/{id}")
    public TeamResponse getTeamById(@PathVariable @Min(1) Long id) {
        var team = teamService.getTeamById(id);
        return dtoMapper.toTeamResponse(team);
    }

    @PreAuthorize("hasRole('TEAMLEADER')")
    @DeleteMapping("/{teamId}")
    public void deleteTeam(@PathVariable @Min(1) Long teamId) {
        teamService.deleteTeam(teamId);
    }

    @GetMapping("/{teamId}/members")
    public List<UserResponse> getTeamUsers(@PathVariable @Min(1) Long teamId) {
        return teamService.getTeamMembers(teamId).stream()
                .map(dtoMapper::toUserResponseBrief)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('TEAMLEADER')")
    @PutMapping("/{teamId}")
    public TeamResponse updateTeam(@PathVariable @Min(1) Long teamId, @RequestBody @Valid TeamRequest request) {
        var team = teamService.updateTeam(teamId, request);
        return dtoMapper.toTeamResponse(team);
    }
}
