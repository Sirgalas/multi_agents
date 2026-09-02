package ru.sergalas.orchestrator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private String taskTemplateName;
    private LocalDateTime createdAt;
    @Builder.Default
    private List<AgentStepResponse> steps = new ArrayList<>();
    @Builder.Default
    private List<McpServerResponse> mcpServers = new ArrayList<>();
    private String zipArchiveUrl;
}