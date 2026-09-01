package ru.sergalas.orchestrator.dto.response;

import java.time.Instant;

public record ProjectSummaryResponse(
    Long id,
    String name,
    String description,
    Instant createdAt,
    int stepCount
) {}