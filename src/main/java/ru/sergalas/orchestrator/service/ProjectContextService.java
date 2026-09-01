package ru.sergalas.orchestrator.service;

import org.springframework.security.core.Authentication;
import ru.sergalas.orchestrator.dto.request.ProjectContextUploadRequest;
import ru.sergalas.orchestrator.dto.response.ProjectContextResponse;

import java.util.List;

public interface ProjectContextService {
    ProjectContextResponse upload(Long projectId, ProjectContextUploadRequest request, Authentication auth);
    List<ProjectContextResponse> getByProject(Long projectId, Authentication auth);
    void delete(Long contextId, Authentication auth);

    /**
     * Формирует единый текстовый контекст проекта для инжекции в промпты.
     */
    String buildPromptContext(Long projectId);
}