package ru.urfu.TeamTaskManager.service;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
import ru.urfu.TeamTaskManager.event.TaskAssignedEvent;
import ru.urfu.TeamTaskManager.exception.ConflictException;
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

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final MailService mailService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Task createTask(TaskRequest request) {
        log.info("Attempt to create task with title: {}", request.getTitle());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .build();

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId()).orElseThrow(() -> {
                log.error("User not found with id: {}", request.getAssignedUserId());
                return new NotFoundException("User not found with id: " + request.getAssignedUserId());
            });
                if(user.getTeam() != null && leader.getTeam() != null && !user.getTeam().equals(leader.getTeam())) {
                    log.warn("Team leader {} cannot assign task to user from different team", username);
                    throw new ForbiddenException("Team leaders can only assign tasks to users from their team");
                }
            task.setAssignedUser(user);
        }

        Task saved = taskRepository.save(task);
        log.info("Task created successfully with id: {}", saved.getId());
        eventPublisher.publishEvent(new TaskAssignedEvent(saved.getId(), saved.getAssignedUser().getId()));
        return saved;
    }

    public Task getTaskById(Long taskId) {
        log.info("Getting task by id: {}", taskId);
        return taskRepository.findById(taskId).orElseThrow(() -> {
            log.error("Task not found with id: {}", taskId);
            return new NotFoundException("Task not found with id: " + taskId);
        });
    }

    public List<Task> getUserTasks(Long userId) {
        log.info("Getting tasks for user id: {}", userId);
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.error("User not found with id: {}", userId);
            return new NotFoundException("User not found with id: " + userId);
        });
        return user.getTasks();
    }

    public Page<Task> getAllTasks(int page, int size) {
        log.info("Getting all tasks with page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return taskRepository.findAll(pageable);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        log.info("Attempt to delete task with id: {}", taskId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        Task task = getTaskById(taskId);
        User assignedUser = task.getAssignedUser();
        if(assignedUser.getTeam() != null && leader.getTeam() != null && !assignedUser.getTeam().equals(leader.getTeam())){
            log.warn("Team leader {} cannot delete task assigned to user from different team", username);
            throw new ForbiddenException("Team leaders can only delete tasks assigned to users from their team");
        }
        taskRepository.delete(task);
        log.info("Task {} deleted", taskId);
    }

    @Transactional
    public Task changeTaskAssignedUser(Long taskId, Long userId) {
        log.info("Attempt to change assigned user for task {} to user {}", taskId, userId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User teamLeader = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        Task task = getTaskById(taskId);
        User previousAssignedUser = task.getAssignedUser();
        if (previousAssignedUser == null) {
            log.warn("Task {} has no assigned user", taskId);
            throw new ConflictException("Task has no assigned user");
        }
        User nextAssignedUser = userRepository.findById(userId).orElseThrow(() -> {
            log.error("User not found with id: {}", userId);
            return new NotFoundException("User not found with id: " + userId);
        });
        if(nextAssignedUser.getTeam() == null || previousAssignedUser.getTeam() == null || teamLeader.getTeam() == null) {
            log.warn("Missing team information for reassigning task {}", taskId);
            throw new ValidationException("User team information is missing, cannot reassign task");
        }
        if(!teamLeader.getTeam().equals(nextAssignedUser.getTeam()) || !teamLeader.getTeam().equals(previousAssignedUser.getTeam())) {
            log.warn("Team leader {} cannot reassign task {} to user from different team", username, taskId);
            throw new ForbiddenException("Team leader can only reassign tasks to users from their team");
        }

        task.setAssignedUser(nextAssignedUser);
        log.info("Task {} assigned user changed to {}", taskId, userId);
        eventPublisher.publishEvent(new TaskAssignedEvent(task.getId(), task.getAssignedUser().getId()));
        return task;
    }

    @Transactional
    public Task updateTaskFromRequest(Long taskId, TaskRequest request) {
        log.info("Attempt to update task with id: {}", taskId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User teamLeader = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        Task existingTask = getTaskById(taskId);
        User assignedUser = existingTask.getAssignedUser();
        if (assignedUser != null && assignedUser.getTeam() != null && teamLeader.getTeam() != null && !assignedUser.getTeam().equals(teamLeader.getTeam())) {
            log.warn("Team leader {} cannot update task assigned to user from different team", username);
            throw new ForbiddenException("Team leaders can only update tasks assigned to users from their team");
        }
        existingTask.setTitle(request.getTitle());
        existingTask.setDescription(request.getDescription());
        existingTask.setDeadline(request.getDeadline());

        if (request.getAssignedUserId() != null) {
            User newAssignedUser = userRepository.findById(request.getAssignedUserId()).orElseThrow(() -> {
                log.error("User not found with id: {}", request.getAssignedUserId());
                return new NotFoundException("User not found with id: " + request.getAssignedUserId());
            });
            if(!newAssignedUser.getTeam().equals(teamLeader.getTeam())) {
                log.warn("Team leader {} cannot assign task to user from different team", username);
                throw new ForbiddenException("Team leaders can only assign tasks to users from their team");
            }
            existingTask.setAssignedUser(newAssignedUser);
        }
        log.info("Task {} updated", taskId);
        eventPublisher.publishEvent(new TaskAssignedEvent(existingTask.getId(), existingTask.getAssignedUser().getId()));
        return existingTask;
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus taskStatus) {
        log.info("Attempt to update status for task {} to {}", taskId, taskStatus);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        Task task = getTaskById(taskId);
        User assignedUser = task.getAssignedUser();
        if (assignedUser == null) {
            log.warn("Task {} has no assigned user", taskId);
            throw new ConflictException("Task has no assigned user");
        }
        if(user.getRole() == Role.MEMBER && Objects.equals(user.getId(), task.getAssignedUser().getId())
        || user.getRole() == Role.TEAMLEADER && task.getAssignedUser().getTeam() == user.getTeam()) {
            task.setTaskStatus(taskStatus);
            log.info("Task {} status updated to {}", taskId, taskStatus);
        }
        else {
            log.warn("User {} cannot update status of task {}", username, taskId);
            throw new ForbiddenException("Users can only update status of their own tasks or team leaders can update status of tasks assigned to users from their team");
        }
        return task;
    }
}
