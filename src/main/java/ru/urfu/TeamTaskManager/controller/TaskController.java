package ru.urfu.TeamTaskManager.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import ru.urfu.TeamTaskManager.dto.mapper.DtoMapper;
import ru.urfu.TeamTaskManager.dto.response.TaskResponse;
import ru.urfu.TeamTaskManager.dto.request.TaskRequest;
import ru.urfu.TeamTaskManager.enums.TaskStatus;
import ru.urfu.TeamTaskManager.service.TaskService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final DtoMapper dtoMapper;

    @PostMapping
    public TaskResponse createTask(@RequestBody TaskRequest request) {
        var task = taskService.createTask(request);
        return dtoMapper.toTaskResponse(task);
    }

    @GetMapping("/{taskId}")
    public TaskResponse getTaskById(@PathVariable Long taskId) {
        var task = taskService.getTaskById(taskId);
        return dtoMapper.toTaskResponse(task);
    }

    @GetMapping("/user/{userId}")
    public List<TaskResponse> getUserTasks(@PathVariable Long userId) {
        var tasks = taskService.getUserTasks(userId);
        return tasks.stream()
                .map(dtoMapper::toTaskResponseBrief)
                .collect(Collectors.toList());
    }

    @GetMapping
    public Page<TaskResponse> getAllTasks(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return taskService.getAllTasks(page, size).map(dtoMapper::toTaskResponseBrief);
    }

    @DeleteMapping("/{taskId}")
    public void deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
    }

    @PutMapping("/{taskId}/assign/{userId}")
    public TaskResponse assignTaskToUser(@PathVariable Long taskId, @PathVariable Long userId) {
        var task = taskService.assignTaskToUser(taskId, userId);
        return dtoMapper.toTaskResponse(task);
    }

    @PutMapping("/{taskId}/reassign/{userId}")
    public TaskResponse changeTaskAssignment(@PathVariable Long taskId, @PathVariable Long userId) {
        var task = taskService.changeTaskAssignedUser(taskId, userId);
        return dtoMapper.toTaskResponse(task);
    }

    @PutMapping("/{taskId}")
    public TaskResponse updateTask(@PathVariable Long taskId, @RequestBody TaskRequest request) {
        var task = taskService.updateTaskFromRequest(taskId, request);
        return dtoMapper.toTaskResponse(task);
    }

    @PutMapping("/{taskId}/status")
    public TaskResponse updateTaskStatus(@PathVariable Long taskId, @RequestBody TaskStatus taskStatus) {
        var task = taskService.updateTaskStatus(taskId, taskStatus);
        return dtoMapper.toTaskResponse(task);
    }
}
