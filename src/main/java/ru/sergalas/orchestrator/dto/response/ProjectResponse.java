package ru.sergalas.orchestrator.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    LocalDateTime createdAt,
    List<ProjectContextResponse> contexts,
    List<McpServerResponse> mcpServers,
    List<AgentStepResponse> steps
) {}