package ru.urfu.TeamTaskManager.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.TeamTaskManager.domain.*;
import ru.urfu.TeamTaskManager.dto.request.TaskRequest;
import ru.urfu.TeamTaskManager.enums.Role;
import ru.urfu.TeamTaskManager.enums.TaskStatus;
import ru.urfu.TeamTaskManager.exception.ForbiddenException;
import ru.urfu.TeamTaskManager.exception.NotFoundException;
import ru.urfu.TeamTaskManager.exception.ValidationException;
import ru.urfu.TeamTaskManager.repository.*;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public Task createTask(TaskRequest request, Long userId) {
        User currentUser = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        if (currentUser.getRole() != Role.TEAMLEADER) {
            throw new ForbiddenException("Only team leaders can create tasks");
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .build();

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId()).orElseThrow(() -> new NotFoundException("User not found with id: " + request.getAssignedUserId()));
            task.setAssignedUser(user);
        }

        return taskRepository.save(task);
    }

    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found with id: " + taskId));
    }

    public List<Task> getUserTasks(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
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
    public Task changeTaskAssignedUser(Long taskId, Long userId, Long teamLeaderId) {
        User teamLeader = userRepository.findById(teamLeaderId).orElseThrow(() -> new NotFoundException("User not found with id: " + teamLeaderId));
        if (teamLeader.getRole() != Role.TEAMLEADER) {
            throw new ValidationException("Only team leaders can reassign tasks");
        }
        Task task = getTaskById(taskId);
        User previousAssignedUser = task.getAssignedUser();
        if (previousAssignedUser == null) {
            throw new ValidationException("Task has no assigned user");
        }
        User nextAssignedUser = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        if(nextAssignedUser.getTeam() == null || previousAssignedUser.getTeam() == null || teamLeader.getTeam() == null) {
            throw new ValidationException("User team information is missing, cannot reassign task");
        }
        if(!teamLeader.getTeam().equals(nextAssignedUser.getTeam()) || !teamLeader.getTeam().equals(previousAssignedUser.getTeam())) {
            throw new ValidationException("Team leader can only reassign tasks to users from their team");
        }

        task.setAssignedUser(nextAssignedUser);
        return task;
    }

    @Transactional
    public Task updateTaskFromRequest(Long taskId, TaskRequest request) {
        Task existingTask = getTaskById(taskId);
        existingTask.setTitle(request.getTitle());
        existingTask.setDescription(request.getDescription());
        existingTask.setDeadline(request.getDeadline());

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId()).orElseThrow(() -> new NotFoundException("User not found with id: " + request.getAssignedUserId()));
            existingTask.setAssignedUser(user);
        }
        return existingTask;
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus taskStatus) {
        Task task = getTaskById(taskId);
        task.setTaskStatus(taskStatus);
        return task;
    }
}
