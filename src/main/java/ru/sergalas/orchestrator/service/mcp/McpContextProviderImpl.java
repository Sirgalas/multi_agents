package ru.sergalas.orchestrator.service.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.enums.TransportType;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpContextProviderImpl implements McpContextProvider {

    private final ProjectMcpServerRepository mcpServerRepository;
    private final RestClient restClient;

    @Override
    public String collectContext(Long projectId) {
        List<ProjectMcpServer> activeServers = mcpServerRepository.findAllByProjectIdAndActiveTrueOrderByCreatedAtAsc(projectId);
        if (activeServers.isEmpty()) {
            return "";
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("\n=== MCP CONTEXT & GUIDELINES ===\n");

        for (ProjectMcpServer server : activeServers) {
            contextBuilder.append("\n--- MCP Server: ").append(server.getName()).append(" (")
                    .append(server.getServerUrl()).append(") ---\n");

            try {
                if (server.getTransportType() == TransportType.SSE || server.getTransportType() == TransportType.HTTP) {
                    String serverDocumentation = fetchHttpMcpResource(server.getServerUrl());
                    if (serverDocumentation != null && !serverDocumentation.isBlank()) {
                        contextBuilder.append(serverDocumentation).append("\n");
                    } else {
                        contextBuilder.append("[MCP Server reachable, but returned empty documentation]\n");
                    }
                } else {
                    contextBuilder.append("[STDIO Transport: configured for local daemon execution]\n");
                }
            } catch (Exception ex) {
                log.warn("Failed to collect context from MCP server {}: {}", server.getServerUrl(), ex.getMessage());
                contextBuilder.append("[Error connecting to MCP server: ").append(ex.getMessage()).append("]\n");
            }
        }
        contextBuilder.append("=== END MCP CONTEXT ===\n\n");
        return contextBuilder.toString();
    }

    @Override
    public boolean isReachable(String serverUrl, TransportType transportType) {
        if (transportType == TransportType.STDIO) {
            return true;
        }
        try {
            var response = restClient.get()
                    .uri(URI.create(serverUrl))
                    .accept(MediaType.ALL)
                    .retrieve()
                    .toBodilessEntity();
            return response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection();
        } catch (Exception e) {
            log.debug("MCP Server {} unreachable: {}", serverUrl, e.getMessage());
            return false;
        }
    }

    private String fetchHttpMcpResource(String serverUrl) {
        try {
            return restClient.get()
                    .uri(URI.create(serverUrl))
                    .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.ALL)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("Unable to fetch documentation from MCP endpoint {}: {}", serverUrl, e.getMessage());
            return null;
        }
    }
}