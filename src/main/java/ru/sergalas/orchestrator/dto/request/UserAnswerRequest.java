package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserAnswerRequest(
    @NotNull(message = "Project ID is required")
    Long projectId,

    @NotNull(message = "Step ID is required")
    Long stepId,

    @NotBlank(message = "Answer cannot be blank")
    String answer
) {}