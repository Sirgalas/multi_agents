package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.sergalas.orchestrator.enums.TransportType;

public record AddMcpServerRequest(
    @NotBlank(message = "Server name is required")
    @Size(max = 255, message = "Server name cannot exceed 255 characters")
    String name,

    @NotBlank(message = "Server URL is required")
    @Size(max = 2048, message = "Server URL cannot exceed 2048 characters")
    String serverUrl,

    @NotNull(message = "Transport type is required")
    TransportType transportType,

    boolean isActive
) {}