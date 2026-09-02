package ru.sergalas.orchestrator.service.agent;

import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;

import java.util.List;
import java.util.Map;

public interface WorkerAgent {
    Map<String, String> generateCode(
            ArchitectSpecification spec,
            List<ProjectMcpServer> mcpServers
    );
}