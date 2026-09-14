package ru.sergalas.orchestrator.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectArchiveResponse {
    private String archivePath;
    private String downloadUrl;
    private Long sizeBytes;
}