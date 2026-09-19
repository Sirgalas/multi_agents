package ru.sergalas.orchestrator.service.mcp;

import ru.sergalas.orchestrator.dto.request.CreateMcpServerRequest;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.enums.McpTarget;

import java.util.List;

public interface McpServerService {
    List<McpServer> getAllServers();
    List<McpServer> getServersByTarget(McpTarget target);
    McpServer createServer(CreateMcpServerRequest request);
    void deleteServer(Long id);
    McpServer getServerById(Long id);
}
