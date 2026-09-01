package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import ru.sergalas.orchestrator.enums.FileType;

public record UploadContextRequest(
    @NotNull(message = "File is required")
    MultipartFile file,

    @NotNull(message = "File type is required")
    FileType fileType
) {}