package ru.urfu.TeamTaskManager.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.TeamTaskManager.domain.*;
import ru.urfu.TeamTaskManager.dto.request.TaskRequest;
import ru.urfu.TeamTaskManager.enums.TaskStatus;
import ru.urfu.TeamTaskManager.repository.*;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public Task createTask(TaskRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be null or empty");
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .build();

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getAssignedUserId()));
            task.setAssignedUser(user);
        }

        return taskRepository.save(task);
    }

    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }

    public List<Task> getUserTasks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return user.getTasks();
    }

    public Page<Task> getAllTasks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return taskRepository.findAll(pageable);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = getTaskById(taskId);
        taskRepository.delete(task);
    }

    @Transactional
    public Task assignTaskToUser(Long taskId, Long userId) {
        Task task = getTaskById(taskId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        task.setAssignedUser(user);
        return taskRepository.save(task);
    }

    @Transactional
    public Task changeTaskAssignedUser(Long taskId, Long userId) {
        return assignTaskToUser(taskId, userId);
    }

    @Transactional
    public Task updateTaskFromRequest(Long taskId, TaskRequest request) {
        Task existingTask = getTaskById(taskId);

        existingTask.setTitle(request.getTitle());
        existingTask.setDescription(request.getDescription());
        existingTask.setDeadline(request.getDeadline());

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getAssignedUserId()));
            existingTask.setAssignedUser(user);
        }

        return taskRepository.save(existingTask);
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus taskStatus) {
        Task task = getTaskById(taskId);
        task.setTaskStatus(taskStatus);
        return taskRepository.save(task);
    }
}
