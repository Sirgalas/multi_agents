package ru.sergalas.orchestrator.service;

import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;

import java.util.List;
import java.util.Optional;

public interface AgentStepService {
    AgentStep createStep(Project project, StepName stepName, String prompt, String modelUsed);
    AgentStep updateStepStatus(Long stepId, StepStatus status, String response);
    List<AgentStepResponse> getStepsByProject(Long projectId);
    Optional<AgentStep> getLatestStep(Long projectId, StepName stepName);
    AgentStep getById(Long stepId);
}