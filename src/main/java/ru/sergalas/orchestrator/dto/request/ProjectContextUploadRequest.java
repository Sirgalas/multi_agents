package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import ru.sergalas.orchestrator.entity.enums.FileType;

public record ProjectContextUploadRequest(
    @NotNull(message = "File must not be null")
    MultipartFile file,

    @NotNull(message = "File type is required")
    FileType fileType
) {}