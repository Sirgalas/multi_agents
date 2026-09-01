package ru.sergalas.orchestrator.dto.response;

import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;

import java.time.LocalDateTime;

public record AgentStepResponse(
    Long id,
    StepName stepName,
    StepStatus stepStatus,
    String prompt,
    String response,
    String modelUsed,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}