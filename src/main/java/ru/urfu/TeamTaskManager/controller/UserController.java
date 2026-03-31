package ru.urfu.TeamTaskManager.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.urfu.TeamTaskManager.dto.mapper.DtoMapper;
import ru.urfu.TeamTaskManager.dto.request.UserRequest;
import ru.urfu.TeamTaskManager.dto.response.UserResponse;
import ru.urfu.TeamTaskManager.dto.response.UserResponseBrief;
import ru.urfu.TeamTaskManager.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final DtoMapper dtoMapper;

    @PostMapping
    public UserResponse createUser(@RequestBody UserRequest request) {
        var user = userService.createUser(request);
        return dtoMapper.toUserResponse(user);
    }

    @PutMapping("/{userId}/team/{teamId}")
    public UserResponse assignUserToTeam(@PathVariable Long userId, @PathVariable Long teamId) {
        var user = userService.assignUserToTeam(userId, teamId);
        return dtoMapper.toUserResponse(user);
    }

    @GetMapping("/team/{teamId}")
    public List<UserResponseBrief> getUsersByTeam(@PathVariable Long teamId) {
        return userService.getUsersByTeam(teamId).stream()
                .map(dtoMapper::toUserResponseBrief)
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<UserResponseBrief> getAll() {
        return userService.getAllUsers().stream()
                .map(dtoMapper::toUserResponseBrief)
                .collect(Collectors.toList());
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable Long userId) {
        var user = userService.getUserById(userId);
        return dtoMapper.toUserResponse(user);
    }

    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public void removeUserFromTeam(@PathVariable Long teamId, @PathVariable Long userId) {
        userService.removeUserFromTeam(teamId, userId);
    }

    @PutMapping("/{userId}")
    public UserResponse updateUser(@PathVariable Long userId, @RequestBody UserRequest request) {
        var user = userService.updateUser(userId, request);
        return dtoMapper.toUserResponse(user);
    }

    @PutMapping("/{currentLeaderId}/transfer-role/{newLeaderId}")
    public UserResponse transferTeamLeaderRole(@PathVariable Long currentLeaderId, @PathVariable Long newLeaderId) {
        var user = userService.transferTeamLeaderRole(currentLeaderId, newLeaderId);
        return dtoMapper.toUserResponse(user);
    }
}
