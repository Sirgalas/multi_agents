package ru.sergalas.orchestrator.service.agent;

import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;
import ru.sergalas.orchestrator.entity.FileStructureTemplate;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.util.Either;

import java.util.List;
import java.util.Map;

public interface ArchitectAgent {
    Either<ArchitectQuestionsResponse, ArchitectSpecification> analyze(
            Long projectId,
            String taskContent,
            FileStructureTemplate fileStructure,
            List<ProjectMcpServer> mcpServers,
            List<ProjectContext> contextFiles
    );

    ArchitectSpecification finalizeSpec(
            String taskContent,
            Map<String, String> answers,
            FileStructureTemplate fileStructure,
            List<ProjectMcpServer> mcpServers
    );
}