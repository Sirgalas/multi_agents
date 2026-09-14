package ru.sergalas.orchestrator.service.project;

import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;

import java.util.List;
import java.util.Optional;

public interface ProjectContextService {
    ProjectContext saveFile(Project project, String fileName, String filePath, String content, FileType fileType, int iteration);
    List<ProjectContext> getContextByProject(Project project);
    Optional<ProjectContext> getLatestContextByType(Project project, FileType fileType);
    List<ProjectContext> getAllByType(Project project, FileType fileType);
}