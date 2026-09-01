package ru.sergalas.orchestrator.dto.response;

import java.util.List;

public record OrchestratorChainResponse(
    Long projectId,
    List<AgentStepResponse> steps,
    String status
) {}