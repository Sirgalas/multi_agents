package ru.sergalas.orchestrator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPromptResponse {
    private Long id;
    private String name;
    private StepName stepName;
    private String prompt;
    private Boolean isFinal;
    private Boolean isDefault;
    private String description;
    private List<McpServer> mcpServers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
