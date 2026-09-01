package ru.sergalas.orchestrator.dto.response;

import ru.sergalas.orchestrator.entity.enums.StepStatus;

import java.util.List;

public record OrchestratorStatusResponse(
    Long projectId,
    List<AgentStepResponse> steps,
    StepStatus overallStatus,
    String pendingQuestion
) {}