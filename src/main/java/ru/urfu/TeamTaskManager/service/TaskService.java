package ru.urfu.TeamTaskManager.service;

import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import ru.urfu.TeamTaskManager.event.TaskDeletedEvent;
import ru.urfu.TeamTaskManager.exception.ConflictException;
import ru.urfu.TeamTaskManager.exception.ForbiddenException;
import ru.urfu.TeamTaskManager.exception.NotFoundException;
import ru.urfu.TeamTaskManager.exception.ValidationException;
import ru.urfu.TeamTaskManager.repository.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private final FileStorageService fileStorageService;

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
        for (File file : task.getFiles()) {
            fileStorageService.deleteFile(file.getFilePath());
        }
        eventPublisher.publishEvent(new TaskDeletedEvent(task.getId(), task.getTitle(), assignedUser.getId()));
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
        eventPublisher.publishEvent(new TaskDeletedEvent(task.getId(), task.getTitle(), previousAssignedUser.getId()));
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

    @Transactional
    public byte[] exportTasksToExcel(Long userId) {
        log.info("Trying export all tasks to Excel");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with id: {}", userId);
            throw new NotFoundException("User not found with userId: " + userId);
        });

        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.error("User not found with id: {}", userId);
            return new NotFoundException("User not found with userId: " + userId);
        });
        if (!((currentUser.getRole() == Role.TEAMLEADER && currentUser.getTeam() != null && user.getTeam() != null && currentUser.getTeam().getId().equals(user.getTeam().getId())) || currentUser.getId().equals(user.getId()))) {
            log.error("User {} cannot export tasks of user {}", currentUser.getId(), user.getId());
            throw new ForbiddenException("You don't have permission to export these tasks");
        }
        List<Task> tasks = user.getTasks();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Задачи");
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            String[] columns = {"id", "название", "описание", "статус", "дедлайн", "исполнитель", "создана"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int stroka = 1;
            for (Task task : tasks) {
                Row row = sheet.createRow(stroka++);
                row.createCell(0).setCellValue(task.getId());
                row.createCell(1).setCellValue(task.getTitle());
                row.createCell(2).setCellValue(task.getDescription() != null ? task.getDescription() : "");
                row.createCell(3).setCellValue(task.getTaskStatus() != null ? task.getTaskStatus().name() : "");
                row.createCell(4).setCellValue(task.getDeadline() != null ? task.getDeadline().toString() : "");
                row.createCell(5).setCellValue(task.getAssignedUser() != null ? task.getAssignedUser().getUsername() : "");
                row.createCell(6).setCellValue(task.getCreated_at() != null ? task.getCreated_at().toString() : "");
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            sheet.createFreezePane(0, 1);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            log.info("Excel export completed successfully");
            return outputStream.toByteArray();
        }
        catch (IOException e) {
            log.error("Failed to export tasks to Excel", e);
            throw new RuntimeException("Failed to export tasks to Excel", e);
        }
    }
}
