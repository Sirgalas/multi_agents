package ru.sergalas.orchestrator.dto.response;

import java.util.List;

public record OrchestrationStatusResponse(
    Long projectId,
    List<AgentStepResponse> steps,
    String overallStatus
) {}