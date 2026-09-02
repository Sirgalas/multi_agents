package ru.sergalas.orchestrator.service;

import ru.sergalas.orchestrator.dto.request.ArchitectQuestionResponse;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.dto.response.GenerationResultResponse;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.util.Either;

public interface OrchestratorService {
    Either<ArchitectQuestionsResponse, Long> startGeneration(ProjectCreateRequest request, String username);
    GenerationResultResponse continueGeneration(ArchitectQuestionResponse response);
    ProjectResponse getProjectStatus(Long projectId);
}