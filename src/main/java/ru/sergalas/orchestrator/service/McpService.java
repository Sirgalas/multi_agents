package ru.sergalas.orchestrator.service;

import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.response.McpResource;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;

import java.util.List;

public interface McpService {
    String fetchMcpContext(ProjectMcpServer server, String topic);
    List<McpResource> listResources(ProjectMcpServer server);
    ProjectMcpServer addMcpServer(Long projectId, McpServerRequest request);
    void toggleMcpServer(Long serverId, boolean active);
}