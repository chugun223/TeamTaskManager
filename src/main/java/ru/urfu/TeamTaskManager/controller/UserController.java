package ru.urfu.TeamTaskManager.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.urfu.TeamTaskManager.dto.mapper.DtoMapper;
import ru.urfu.TeamTaskManager.dto.request.UserRequest;
import ru.urfu.TeamTaskManager.dto.response.UserResponse;
import ru.urfu.TeamTaskManager.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;

    private final DtoMapper dtoMapper;

    @PreAuthorize("hasRole('TEAMLEADER')")
    @PutMapping("/{userId}/team/{teamId}")
    public UserResponse assignUserToTeam(@PathVariable @Min(1) Long userId, @PathVariable Long teamId) {
        var user = userService.assignUserToTeam(userId, teamId);
        return dtoMapper.toUserResponse(user);
    }

    @GetMapping("/team/{teamId}")
    public List<UserResponse> getUsersByTeam(@PathVariable @Min(1) Long teamId) {
        return userService.getUsersByTeam(teamId).stream()
                .map(dtoMapper::toUserResponseBrief)
                .collect(Collectors.toList());
    }

    @GetMapping
    public Page<UserResponse> getAll(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return userService.getAllUsers(page, size).map(dtoMapper::toUserResponseBrief);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable @Min(1) Long userId) {
        var user = userService.getUserById(userId);
        return dtoMapper.toUserResponse(user);
    }

    @DeleteMapping
    public void deleteUser() {
        userService.deleteUser();
    }

    @PreAuthorize("hasAnyRole('TEAMLEADER','MEMBER')")
    @DeleteMapping("/members/{userId}")
    public void removeUserFromTeam(@PathVariable @Min(1) Long userId) {
        userService.removeUserFromTeam(userId);
    }

    @PutMapping
    public UserResponse updateUser(@RequestBody @Valid UserRequest request) {
        var user = userService.updateUser(request);
        return dtoMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('TEAMLEADER')")
    @PutMapping("/transfer-role/{newLeaderId}")
    public UserResponse transferTeamLeaderRole(@PathVariable @Min(1) Long newLeaderId) {
        var user = userService.transferTeamLeaderRole(newLeaderId);
        return dtoMapper.toUserResponse(user);
    }
}
