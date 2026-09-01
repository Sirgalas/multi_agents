package ru.sergalas.orchestrator.service;

import org.springframework.security.core.Authentication;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.response.McpServerResponse;

import java.util.List;

public interface McpServerService {
    List<McpServerResponse> getByProject(Long projectId, Authentication auth);
    McpServerResponse create(Long projectId, McpServerRequest request, Authentication auth);
    McpServerResponse update(Long serverId, McpServerRequest request, Authentication auth);
    void delete(Long serverId, Authentication auth);
    void toggleActive(Long serverId, Authentication auth);

    /**
     * Собирает контекст правил/документации из всех активных MCP-серверов проекта.
     * Используется OrchestratorService перед построением промптов.
     */
    String collectMcpContext(Long projectId);
}