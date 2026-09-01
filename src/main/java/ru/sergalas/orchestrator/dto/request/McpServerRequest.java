package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import ru.sergalas.orchestrator.entity.enums.TransportType;

public record McpServerRequest(
    @NotBlank(message = "Server name is required")
    @Size(max = 255, message = "Server name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Server URL is required")
    @Size(max = 1024, message = "Server URL must not exceed 1024 characters")
    @URL(message = "Must be a valid URL")
    @Pattern(regexp = "^https?://.*$", message = "Server URL must start with http:// or https://")
    String serverUrl,

    @NotNull(message = "Transport type is required")
    TransportType transportType,

    boolean active
) {}