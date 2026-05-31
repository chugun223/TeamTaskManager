package ru.urfu.TeamTaskManager.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileResponse {
    private Long id;
    private String filename;
    private String contentType;
    private Long size;
}

