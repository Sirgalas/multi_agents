package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.sergalas.orchestrator.enums.StepName;

import java.util.List;

public record RunOrchestrationRequest(
    @NotNull(message = "Project ID is required")
    Long projectId,

    @NotEmpty(message = "At least one orchestration step must be selected")
    List<StepName> steps
) {}