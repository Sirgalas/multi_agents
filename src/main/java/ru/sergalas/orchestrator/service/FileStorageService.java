package ru.sergalas.orchestrator.service;

import org.springframework.web.multipart.MultipartFile;
import ru.sergalas.orchestrator.model.ProjectContext;
import ru.sergalas.orchestrator.model.enums.FileType;

import java.util.List;

public interface FileStorageService {
    ProjectContext store(Long projectId, MultipartFile file, FileType fileType);
    List<ProjectContext> listForProject(Long projectId);
    void delete(Long contextId, Long projectId);
}