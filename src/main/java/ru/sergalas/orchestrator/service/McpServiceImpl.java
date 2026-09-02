package ru.sergalas.orchestrator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.response.McpResource;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpServiceImpl implements McpService {

    private final RestTemplate restTemplate;
    private final ProjectMcpServerRepository mcpServerRepository;
    private final ProjectRepository projectRepository;

    @Override
    public String fetchMcpContext(ProjectMcpServer server, String topic) {
        if (!Boolean.TRUE.equals(server.getIsActive())) {
            return "";
        }

        try {
            String url = switch (server.getTransportType()) {
                case HTTP -> server.getServerUrl() + (topic != null && !topic.isBlank() ? "/resources/" + topic : "");
                case SSE -> fetchViaSse(server, topic);
                case STDIO -> throw new UnsupportedOperationException("STDIO transport is not supported yet");
            };

            log.info("Fetching MCP context from URL: {}", url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody() != null ? response.getBody() : "";
        } catch (Exception e) {
            log.warn("Failed to fetch MCP context from {} ({}): {}", server.getName(), server.getServerUrl(), e.getMessage());
            return "Standard Best Practices for " + server.getName() + ": Follow idiomatic Clean Architecture conventions.";
        }
    }

    private String fetchViaSse(ProjectMcpServer server, String topic) {
        return "SSE Mock Streamed Context for " + server.getName();
    }

    @Override
    public List<McpResource> listResources(ProjectMcpServer server) {
        return List.of(
            McpResource.builder()
                .uri(server.getServerUrl() + "/guidelines")
                .name(server.getName() + " Guidelines")
                .description("Documentation and rules from " + server.getName())
                .mimeType("text/markdown")
                .build()
        );
    }

    @Override
    @Transactional
    public ProjectMcpServer addMcpServer(Long projectId, McpServerRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + projectId));

        ProjectMcpServer server = ProjectMcpServer.builder()
                .project(project)
                .name(request.getName())
                .serverUrl(request.getServerUrl())
                .transportType(request.getTransportType())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mcpServerRepository.save(server);
    }

    @Override
    @Transactional
    public void toggleMcpServer(Long serverId, boolean active) {
        ProjectMcpServer server = mcpServerRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found with id: " + serverId));
        server.setIsActive(active);
        mcpServerRepository.save(server);
    }
}