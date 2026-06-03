package ru.urfu.TeamTaskManager.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class TaskController {
    private final TaskService taskService;
    private final DtoMapper dtoMapper;

    @PreAuthorize("hasRole('TEAMLEADER')")
    @PostMapping
    public TaskResponse createTask(@RequestBody @Valid TaskRequest request) {
        var task = taskService.createTask(request);
        return dtoMapper.toTaskResponse(task);
    }

    @GetMapping("/{taskId}")
    public TaskResponse getTaskById(@PathVariable @Min(1) Long taskId) {
        var task = taskService.getTaskById(taskId);
        return dtoMapper.toTaskResponse(task);
    }

    @GetMapping("/user/{userId}")
    public List<TaskResponse> getUserTasks(@PathVariable @Min(1) Long userId) {
        var tasks = taskService.getUserTasks(userId);
        return tasks.stream()
                .map(dtoMapper::toTaskResponseBrief)
                .collect(Collectors.toList());
    }

    @GetMapping
    public Page<TaskResponse> getAllTasks(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return taskService.getAllTasks(page, size).map(dtoMapper::toTaskResponseBrief);
    }

    @PreAuthorize("hasRole('TEAMLEADER')")
    @DeleteMapping("/{taskId}")
    public void deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
    }

    @PreAuthorize("hasRole('TEAMLEADER')")
    @PutMapping("/{taskId}/reassign/{userId}")
    public TaskResponse changeTaskAssignment(@PathVariable @Min(1) Long taskId, @PathVariable @Min(1) Long userId) {
        var task = taskService.changeTaskAssignedUser(taskId, userId);
        return dtoMapper.toTaskResponse(task);
    }

    @PreAuthorize("hasRole('TEAMLEADER')")
    @PutMapping("/{taskId}")
    public TaskResponse updateTask(@PathVariable @Min(1) Long taskId, @RequestBody @Valid TaskRequest request) {
        var task = taskService.updateTaskFromRequest(taskId, request);
        return dtoMapper.toTaskResponse(task);
    }

    @PreAuthorize("hasAnyRole('TEAMLEADER','MEMBER')")
    @PutMapping("/{taskId}/status")
    public TaskResponse updateTaskStatus(@PathVariable @Min(1) Long taskId, @RequestBody TaskStatus taskStatus) {
        var task = taskService.updateTaskStatus(taskId, taskStatus);
        return dtoMapper.toTaskResponse(task);
    }

    @PreAuthorize("hasAnyRole('TEAMLEADER','MEMBER')")
    @GetMapping("/export/excel/{userId}")
    public ResponseEntity<byte[]> exportTasksToExcel(@PathVariable @Min(1) Long userId) {
        byte[] excelData = taskService.exportTasksToExcel(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tasksExportExcel.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(excelData.length)
                .body(excelData);
    }
}
