package ru.sergalas.orchestrator.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.response.McpServerResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.security.UserPrincipal;
import ru.sergalas.orchestrator.service.McpServerService;
import ru.sergalas.orchestrator.service.ProjectService;
import ru.sergalas.orchestrator.service.mcp.McpContextProvider;

import java.util.List;

@Service
@RequiredArgsConstructor
public class McpServerServiceImpl implements McpServerService {

    private final ProjectMcpServerRepository mcpServerRepository;
    private final ProjectService projectService;
    private final McpContextProvider mcpContextProvider;

    @Override
    @Transactional(readOnly = true)
    public List<McpServerResponse> getByProject(Long projectId, Authentication auth) {
        projectService.getProjectEntity(projectId, auth);
        return mcpServerRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public McpServerResponse create(Long projectId, McpServerRequest request, Authentication auth) {
        Project project = projectService.getProjectEntity(projectId, auth);

        ProjectMcpServer server = ProjectMcpServer.builder()
                .project(project)
                .name(request.name())
                .serverUrl(request.serverUrl())
                .transportType(request.transportType())
                .active(request.active())
                .build();

        ProjectMcpServer saved = mcpServerRepository.save(server);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public McpServerResponse update(Long serverId, McpServerRequest request, Authentication auth) {
        ProjectMcpServer server = getAndVerifyServer(serverId, auth);
        server.setName(request.name());
        server.setServerUrl(request.serverUrl());
        server.setTransportType(request.transportType());
        server.setActive(request.active());

        ProjectMcpServer updated = mcpServerRepository.save(server);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long serverId, Authentication auth) {
        ProjectMcpServer server = getAndVerifyServer(serverId, auth);
        mcpServerRepository.delete(server);
    }

    @Override
    @Transactional
    public void toggleActive(Long serverId, Authentication auth) {
        ProjectMcpServer server = getAndVerifyServer(serverId, auth);
        server.setActive(!server.isActive());
        mcpServerRepository.save(server);
    }

    @Override
    @Transactional(readOnly = true)
    public String collectMcpContext(Long projectId) {
        return mcpContextProvider.collectContext(projectId);
    }

    private ProjectMcpServer getAndVerifyServer(Long serverId, Authentication auth) {
        ProjectMcpServer server = mcpServerRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + serverId));

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        if (principal.getRole() != Role.ROLE_ADMIN && !server.getProject().getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Access denied to MCP server configuration");
        }
        return server;
    }

    private McpServerResponse mapToResponse(ProjectMcpServer server) {
        return new McpServerResponse(
                server.getId(),
                server.getName(),
                server.getServerUrl(),
                server.getTransportType(),
                server.isActive(),
                server.getCreatedAt()
        );
    }
}