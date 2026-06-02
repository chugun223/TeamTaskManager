package ru.urfu.TeamTaskManager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.TeamTaskManager.dto.response.FileResponse;
import ru.urfu.TeamTaskManager.domain.File;
import ru.urfu.TeamTaskManager.service.FileService;
import ru.urfu.TeamTaskManager.service.FileStorageService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAnyRole('TEAMLEADER','MEMBER')")
    @PostMapping(value = "/tasks/{taskId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<FileResponse> uploadFiles(@PathVariable Long taskId, @RequestParam("files") MultipartFile[] files) {
        List<File> files1 = fileService.uploadFiles(taskId, files);
        return files1.stream().map(a -> FileResponse.builder()
                .id(a.getId())
                .filename(a.getFilename())
                .contentType(a.getContentType())
                .size(a.getSize())
                .build()).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('TEAMLEADER','MEMBER')")
    @GetMapping("/tasks/{taskId}/files")
    public List<FileResponse> listFiles(@PathVariable Long taskId) {
        return fileService.getFilesForTask(taskId).stream().map(a -> FileResponse.builder()
                .id(a.getId())
                .filename(a.getFilename())
                .contentType(a.getContentType())
                .size(a.getSize())
                .build()).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('TEAMLEADER','MEMBER')")
    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws IOException {
        File file = fileService.getFile(id);

        Resource resource = fileService.downloadFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "file; filename=\"" + file.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType()))
                .contentLength(file.getSize())
                .body(resource);
    }

    @PreAuthorize("hasAnyRole('TEAMLEADER','MEMBER')")
    @DeleteMapping("/files/{id}")
    public void deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
    }
}