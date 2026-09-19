package ru.sergalas.orchestrator.service.orchestrator;

import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;

public interface OrchestratorService {
    void executePipeline(Long projectId);
    void executePipeline(Long projectId, String fromStep);
    void approveSpecAndContinue(Long projectId);
    void restartDevelopment(Long projectId);
    ProjectArchiveResponse archiveProject(Long projectId);
}