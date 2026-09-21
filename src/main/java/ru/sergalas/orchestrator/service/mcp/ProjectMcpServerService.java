package ru.sergalas.orchestrator.service.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.request.AddMcpServerRequest;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.McpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMcpServerService {

    private final ProjectMcpServerRepository projectMcpServerRepository;
    private final McpServerRepository mcpServerRepository;
    private final ProjectService projectService;

    @Transactional
    public ProjectMcpServer registerServer(AddMcpServerRequest request) {
        Project project = projectService.getProjectById(request.getProjectId());

        McpServer server;
        if (request.getServerId() != null) {
            server = mcpServerRepository.findById(request.getServerId())
                    .orElseThrow(() -> new IllegalArgumentException("MCP сервер не найден с ID: " + request.getServerId()));
        } else {
            if (request.getName() == null || request.getName().isBlank()) {
                throw new IllegalArgumentException("Название MCP сервера обязательно");
            }
            if (request.getServerUrl() == null || request.getServerUrl().isBlank()) {
                throw new IllegalArgumentException("URL MCP сервера обязателен");
            }

            server = mcpServerRepository.findByNameIgnoreCase(request.getName().trim())
                    .orElseGet(() -> {
                        McpServer newServer = McpServer.builder()
                                .name(request.getName().trim())
                                .url(request.getServerUrl().trim())
                                .target(request.getTarget() != null ? request.getTarget() : ru.sergalas.orchestrator.entity.enums.McpTarget.COMMON)
                                .token(request.getToken() != null && !request.getToken().isBlank() ? request.getToken().trim() : null)
                                .description(request.getDescription() != null && !request.getDescription().isBlank() ? request.getDescription().trim() : null)
                                .build();
                        return mcpServerRepository.save(newServer);
                    });
        }

        if (projectMcpServerRepository.existsByProjectAndMcpServer(project, server)) {
            return projectMcpServerRepository.findAllByProject(project).stream()
                    .filter(s -> s.getMcpServer() != null && s.getMcpServer().getId().equals(server.getId()))
                    .findFirst()
                    .orElseThrow();
        }

        ProjectMcpServer link = ProjectMcpServer.builder()
                .project(project)
                .mcpServer(server)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return projectMcpServerRepository.save(link);
    }

    @Transactional(readOnly = true)
    public List<ProjectMcpServer> getServersByProject(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        return projectMcpServerRepository.findAllByProject(project);
    }

    @Transactional
    public void toggleActive(Long serverId) {
        projectMcpServerRepository.findById(serverId).ifPresent(s -> {
            s.setIsActive(!s.getIsActive());
            projectMcpServerRepository.save(s);
        });
    }

    @Transactional
    public void deleteServer(Long serverId) {
        projectMcpServerRepository.deleteById(serverId);
    }
}