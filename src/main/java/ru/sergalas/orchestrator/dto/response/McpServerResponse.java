package ru.sergalas.orchestrator.dto.response;

import ru.sergalas.orchestrator.entity.enums.TransportType;

import java.time.LocalDateTime;

public record McpServerResponse(
    Long id,
    String name,
    String serverUrl,
    TransportType transportType,
    boolean active,
    LocalDateTime createdAt
) {}