package ru.urfu.TeamTaskManager.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.Objects;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public Task createTask(TaskRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .build();

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId()).orElseThrow(() -> new NotFoundException("User not found with id: " + request.getAssignedUserId()));
                if(user.getTeam() != null && leader.getTeam() != null && !user.getTeam().equals(leader.getTeam())) {
                    throw new ForbiddenException("Team leaders can only assign tasks to users from their team");
                }
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        Task task = getTaskById(taskId);
        User assignedUser = task.getAssignedUser();
        if(assignedUser.getTeam() != null && leader.getTeam() != null && !assignedUser.getTeam().equals(leader.getTeam())){
            throw new ForbiddenException("Team leaders can only delete tasks assigned to users from their team");
        }
        taskRepository.delete(task);
    }

    @Transactional
    public Task changeTaskAssignedUser(Long taskId, Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User teamLeader = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User teamLeader = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        Task existingTask = getTaskById(taskId);
        User assignedUser = existingTask.getAssignedUser();
        if (assignedUser != null && assignedUser.getTeam() != null && teamLeader.getTeam() != null && !assignedUser.getTeam().equals(teamLeader.getTeam())) {
            throw new ForbiddenException("Team leaders can only update tasks assigned to users from their team");
        }
        existingTask.setTitle(request.getTitle());
        existingTask.setDescription(request.getDescription());
        existingTask.setDeadline(request.getDeadline());

        if (request.getAssignedUserId() != null) {
            User newAssignedUser = userRepository.findById(request.getAssignedUserId()).orElseThrow(() -> new NotFoundException("User not found with id: " + request.getAssignedUserId()));
            if(!newAssignedUser.getTeam().equals(teamLeader.getTeam())) {
                throw new ForbiddenException("Team leaders can only assign tasks to users from their team");
            }
            existingTask.setAssignedUser(newAssignedUser);
        }
        return existingTask;
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus taskStatus) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        Task task = getTaskById(taskId);
        User assignedUser = task.getAssignedUser();
        if (assignedUser == null) {
            throw new ValidationException("Task has no assigned user");
        }
        if(user.getRole() == Role.MEMBER && Objects.equals(user.getId(), task.getAssignedUser().getId())
        || user.getRole() == Role.TEAMLEADER && task.getAssignedUser().getTeam() == user.getTeam()) {
            task.setTaskStatus(taskStatus);
        }
        else {
            throw new ForbiddenException("Users can only update status of their own tasks or team leaders can update status of tasks assigned to users from their team");
        }
        return task;
    }
}
