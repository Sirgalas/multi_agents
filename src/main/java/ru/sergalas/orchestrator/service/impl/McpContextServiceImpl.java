package ru.sergalas.orchestrator.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.sergalas.orchestrator.enums.StepName;
import ru.sergalas.orchestrator.model.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.service.McpContextService;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpContextServiceImpl implements McpContextService {

    private final ProjectMcpServerRepository mcpServerRepository;
    private final WebClient webClient;

    @Override
    public String fetchMcpContext(Long projectId, StepName agentRole) {
        List<ProjectMcpServer> activeServers = mcpServerRepository.findAllByProjectIdAndIsActiveTrue(projectId);
        if (activeServers.isEmpty()) {
            return "";
        }

        StringBuilder contextBuilder = new StringBuilder();

        for (ProjectMcpServer server : activeServers) {
            try {
                log.debug("Fetching MCP context from [{}] for role [{}]", server.getServerUrl(), agentRole);
                String response = webClient.get()
                        .uri(server.getServerUrl())
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(5))
                        .onErrorReturn("[MCP Server at " + server.getServerUrl() + " did not respond]")
                        .block();

                if (response != null && !response.isBlank()) {
                    contextBuilder.append("--- MCP SERVER [").append(server.getName()).append("] ---\n")
                            .append(response).append("\n\n");
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch context from MCP server [{}]: {}", server.getName(), ex.getMessage());
            }
        }

        return contextBuilder.toString();
    }
}