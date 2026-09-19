package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.entity.enums.TransportType;

import java.util.Map;

@Data
public class AddMcpServerRequest {
    
    @NotNull(message = "ID проекта обязателен")
    private Long projectId;
    
    @NotBlank(message = "Название сервера обязательно")
    private String name;
    
    @NotBlank(message = "URL сервера обязателен")
    private String serverUrl;
    
    private TransportType transportType = TransportType.SSE;
    
    private McpTarget target = McpTarget.COMMON;

    private String token;

    private Boolean isActive = true;
    
    private Map<String, Object> config;
}