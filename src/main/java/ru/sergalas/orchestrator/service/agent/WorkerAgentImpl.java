package ru.sergalas.orchestrator.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.service.McpService;
import ru.sergalas.orchestrator.util.FileParser;
import ru.sergalas.orchestrator.util.PromptBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WorkerAgentImpl implements WorkerAgent {

    private final ChatModel workerChatModel;
    private final McpService mcpService;

    public WorkerAgentImpl(
            @Qualifier("workerChatModel") ChatModel workerChatModel,
            McpService mcpService
    ) {
        this.workerChatModel = workerChatModel;
        this.mcpService = mcpService;
    }

    @Override
    public Map<String, String> generateCode(ArchitectSpecification spec, List<ProjectMcpServer> mcpServers) {
        Map<String, String> mcpContexts = new HashMap<>();
        if (mcpServers != null) {
            for (ProjectMcpServer server : mcpServers) {
                if (Boolean.TRUE.equals(server.getIsActive())) {
                    mcpContexts.put(server.getServerUrl(), mcpService.fetchMcpContext(server, "code_generation"));
                }
            }
        }

        String prompt = """
                You are a Senior Lead Developer (Gemini 3.7 Flash High).
                Implement the complete Java 21 Spring Boot 3.4 codebase based strictly on the Architect's specification.
                
                REQUIREMENTS:
                1. Strict Java 21 syntax (records, pattern matching, sealed classes where applicable).
                2. Clean Architecture and DDD principles.
                3. JPA Entities strictly in package: `ru.sergalas.orchestrator.entity` (and enums in `ru.sergalas.orchestrator.entity.enums`).
                4. Every file MUST be wrapped in:
                [FILE: relative/path/to/File.java]
                ```java
                // Code here
                ```
                
                ARCHITECT SPECIFICATION:
                %s
                
                FILE STRUCTURE TREE:
                %s
                
                %s
                
                Generate all domain entities, repositories, services, controllers, DTOs, and configuration classes now.
                """.formatted(
                spec.getArchitectureSpec(),
                spec.getStructureTree(),
                PromptBuilder.buildMcpContextSection(mcpServers, mcpContexts)
        );

        String response = workerChatModel.call(prompt);
        log.debug("Worker Agent code generation completed");

        return FileParser.parseFiles(response);
    }
}