package ru.sergalas.orchestrator.service;

import ru.sergalas.orchestrator.enums.StepName;

public interface McpContextService {
    String fetchMcpContext(Long projectId, StepName agentRole);
}