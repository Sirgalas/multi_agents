package ru.sergalas.orchestrator.dto.response;

import lombok.Builder;
import lombok.Data;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private ProjectStatus status;
    private String taskTemplateName;
    private String archivePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}