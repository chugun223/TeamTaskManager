package ru.urfu.TeamTaskManager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.urfu.TeamTaskManager.dto.mapper.DtoMapper;
import ru.urfu.TeamTaskManager.dto.request.UserRequest;
import ru.urfu.TeamTaskManager.dto.response.UserResponse;
import ru.urfu.TeamTaskManager.service.UserService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final DtoMapper dtoMapper;

    @PostMapping("/register")
    public UserResponse register(@RequestBody @Valid UserRequest request) {
        return dtoMapper.toUserResponse(userService.createUser(request));
    }
}
