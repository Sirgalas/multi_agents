package ru.sergalas.orchestrator.service.orchestrator;

import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;

public interface OrchestratorService {
    void executePipeline(Long projectId);
    ProjectArchiveResponse archiveProject(Long projectId);
}