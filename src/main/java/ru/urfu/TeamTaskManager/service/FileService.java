package ru.urfu.TeamTaskManager.service;

import lombok.AllArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.TeamTaskManager.domain.File;
import ru.urfu.TeamTaskManager.domain.Task;
import ru.urfu.TeamTaskManager.domain.User;
import ru.urfu.TeamTaskManager.enums.Role;
import ru.urfu.TeamTaskManager.exception.NotFoundException;
import ru.urfu.TeamTaskManager.exception.ForbiddenException;
import ru.urfu.TeamTaskManager.repository.FileRepository;
import ru.urfu.TeamTaskManager.repository.TaskRepository;
import ru.urfu.TeamTaskManager.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
@AllArgsConstructor
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public List<File> uploadFiles(Long taskId, MultipartFile[] files) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requestingUsername = auth.getName();
        User requester = userRepository.findByUsername(requestingUsername).orElseThrow(() -> new NotFoundException("User not found with username: " + requestingUsername));
        log.info("Attempt to upload files for taskId={}", taskId);
        if (files.length == 0) {
            log.error("Attempt to upload files without providing any files");
            throw new IllegalArgumentException("At least one file is required");
        }
        if(requester.getTeam() == null){
            log.error("User {} does not belong to any team, cannot upload files for taskId={}", requestingUsername, taskId);
            throw new ForbiddenException("User does not belong to any team");
        }

        Task task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found with id: " + taskId));

        if(task.getAssignedUser() == null){
            log.error("Task with id={} is not assigned to any user, cannot upload files", taskId);
            throw new ForbiddenException("Task is not assigned to any user");
        }
        if(!((requester.getRole() == Role.TEAMLEADER && requester.getTeam() == task.getAssignedUser().getTeam()) || task.getAssignedUser().getId() == requester.getId())){
            log.error("User {} is not allowed to upload files for taskId={}", requestingUsername, taskId);
            throw new ForbiddenException("User is not allowed to upload files for this task");
        }

        return Arrays.stream(files)
                .map(f -> {
                    try {
                        String filePath = fileStorageService.saveFile(f);
                        File a = File.builder()
                                .filename(f.getOriginalFilename())
                                .contentType(f.getContentType())
                                .size(f.getSize())
                                .filePath(filePath)
                                .task(task)
                                .build();
                        task.getFiles().add(a);
                        log.info("File {} uploaded successfully for taskId={}", f.getOriginalFilename(), taskId);
                        return fileRepository.save(a);
                    } catch (IOException e) {
                        log.error("Failed to save file", e);
                        throw new RuntimeException("Failed to save file: " + e.getMessage());
                    }
                }).toList();
    }

    public List<File> getFilesForTask(Long taskId) {
        log.info("Attempt to get files for taskId={}", taskId);
        if (!taskRepository.existsById(taskId)){
            log.error("Attempt to get files for non exist taskId={}", taskId);
            throw new NotFoundException("Task not found with id: " + taskId);
        }
        log.info("Files for taskId={} got successfully", taskId);
        return fileRepository.findByTaskId(taskId);
    }

    public File getFile(Long fileId) {
        log.info("Attempt to get file with id={}", fileId);
        File a = fileRepository.findById(fileId).orElseThrow(() -> {
            log.error("File not found with id={}", fileId);
            return new NotFoundException("File not found with id: " + fileId);
        });
        log.info("File with id={} got successfully", fileId);
        return a;
    }

    public Resource downloadFile(Long fileId) throws IOException {
        log.info("Attempt to download file with id={}", fileId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requestingUsername = auth.getName();
        File file = getFile(fileId);
        Task task = file.getTask();
        if (task == null){
            log.error("Task for file with id={} not found", fileId);
            throw new NotFoundException("Task for file not found");
        }

        User requester = userRepository.findByUsername(requestingUsername).orElseThrow(() -> new NotFoundException("User not found with username: " + requestingUsername));

        if (task.getAssignedUser() == null || (requester.getRole() != Role.TEAMLEADER && !task.getAssignedUser().getId().equals(requester.getId())) || (requester.getRole() == Role.TEAMLEADER && requester.getTeam() != task.getAssignedUser().getTeam())) {
            log.error("User {} is not allowed to download file with id={}", requestingUsername, fileId);
            throw new ForbiddenException("User is not allowed to download this file");
        }
        Resource resource = new FileSystemResource(Paths.get(file.getFilePath()));
        if (!resource.exists()) {
            log.error("File for file with id={} not found on disk", fileId);
            throw new IOException("File not found: " + file.getFilePath());
        }
        log.info("File with id={} downloaded successfully by user {}", fileId, requestingUsername);
        return resource;
    }

    @Transactional
    public void deleteFile(Long fileId) {
        log.info("Attempt to delete file with id={}", fileId);
        File file = getFile(fileId);
        fileStorageService.deleteFile(file.getFilePath());
        Task task = file.getTask();
        if (task != null) {
            task.getFiles().remove(file);
        }
        else{
            log.info("Task for file with id={} not found, skipping task update", fileId);
        }
        fileRepository.delete(file);
        log.info("File {} deleted", fileId);
    }
}


