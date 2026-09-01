package ru.sergalas.orchestrator.dto.response;

import ru.sergalas.orchestrator.entity.enums.FileType;

import java.time.LocalDateTime;

public record ProjectContextResponse(
    Long id,
    Long projectId,
    String fileName,
    FileType fileType,
    String fileContent,
    LocalDateTime createdAt
) {}