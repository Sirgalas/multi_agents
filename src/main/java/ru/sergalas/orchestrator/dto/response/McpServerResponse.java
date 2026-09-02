package ru.sergalas.orchestrator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sergalas.orchestrator.entity.enums.TransportType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerResponse {
    private Long id;
    private String name;
    private String serverUrl;
    private TransportType transportType;
    private Boolean isActive;
}