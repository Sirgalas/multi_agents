package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreateRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    private Long taskTemplateId;

    @NotBlank(message = "Task content cannot be empty")
    private String taskContent;

    @Builder.Default
    private List<McpServerRequest> mcpServers = new ArrayList<>();
}