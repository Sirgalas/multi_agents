package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sergalas.orchestrator.entity.enums.TransportType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerRequest {

    @NotBlank(message = "Server name is required")
    private String name;

    @NotBlank(message = "Server URL is required")
    private String serverUrl;

    @NotNull(message = "Transport type is required")
    @Builder.Default
    private TransportType transportType = TransportType.HTTP;

    @Builder.Default
    private Boolean isActive = true;
}