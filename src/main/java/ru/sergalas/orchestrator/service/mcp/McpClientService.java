package ru.sergalas.orchestrator.service.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpClientService {

    private final ProjectMcpServerRepository mcpServerRepository;
    private final RestClient restClient;

    /**
     * Aggregates context and best practices from all active project MCP servers.
     */
    public String aggregateMcpContext(Project project) {
        List<ProjectMcpServer> activeServers = mcpServerRepository.findAllByProjectAndIsActiveTrue(project);
        if (activeServers.isEmpty()) {
            return "";
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("\n=== MCP ARCHITECTURAL RULES & CONVENTIONS ===\n");

        for (ProjectMcpServer server : activeServers) {
            try {
                log.info("Fetching guidelines from MCP server: {} ({})", server.getName(), server.getServerUrl());
                // Simulating or calling SSE/HTTP endpoint
                String docResponse = fetchServerDocumentation(server.getServerUrl());
                contextBuilder.append("\n--- Source: ").append(server.getName()).append(" ---\n");
                contextBuilder.append(docResponse).append("\n");
            } catch (Exception e) {
                log.warn("Could not retrieve MCP context from {}: {}", server.getServerUrl(), e.getMessage());
                contextBuilder.append("\n--- Source: ").append(server.getName()).append(" [Fallback Guidelines] ---\n");
                contextBuilder.append("Follow Clean Architecture, idiomatic Java 21 patterns, Spring Boot 3 standards, and proper error handling.\n");
            }
        }
        contextBuilder.append("============================================\n");
        return contextBuilder.toString();
    }

    private String fetchServerDocumentation(String url) {
        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return "Standard project guidelines: use record classes, pattern matching, constructor injection, strict Clean Architecture layers.";
        }
    }
}