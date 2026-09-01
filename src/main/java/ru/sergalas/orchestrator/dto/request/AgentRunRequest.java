package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotNull;
import ru.sergalas.orchestrator.enums.AgentRole;

public record AgentRunRequest(
    @NotNull(message = "ID проекта обязателен")
    Long projectId,

    AgentRole startFrom,

    String userClarification
) {}