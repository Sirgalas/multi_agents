package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.sergalas.orchestrator.enums.FileType;

public record FileUploadRequest(
    @NotBlank(message = "Имя файла обязательно")
    String fileName,

    @NotBlank(message = "Содержимое файла обязательно")
    String fileContent,

    @NotNull(message = "Тип файла обязателен")
    FileType fileType
) {}