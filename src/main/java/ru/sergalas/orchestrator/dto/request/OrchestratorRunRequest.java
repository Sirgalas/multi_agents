package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotNull;
import ru.sergalas.orchestrator.entity.enums.StepName;

public record OrchestratorRunRequest(
    @NotNull(message = "Project ID is required")
    Long projectId,

    StepName startFromStep
) {}