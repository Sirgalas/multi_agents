package ru.sergalas.orchestrator.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.request.AddMcpServerRequest;
import ru.sergalas.orchestrator.exception.ProjectNotFoundException;
import ru.sergalas.orchestrator.exception.ResourceNotFoundException;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.ProjectMcpServerService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMcpServerServiceImpl implements ProjectMcpServerService {

    private final ProjectMcpServerRepository mcpServerRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public ProjectMcpServer addServer(Long projectId, Long userId, AddMcpServerRequest req) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ProjectMcpServer server = ProjectMcpServer.builder()
                .project(project)
                .name(req.name())
                .serverUrl(req.serverUrl())
                .transportType(req.transportType())
                .isActive(req.isActive())
                .build();

        log.info("Registered MCP Server [{}] at [{}] for project [{}]", req.name(), req.serverUrl(), projectId);
        return mcpServerRepository.save(server);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMcpServer> getActiveServers(Long projectId) {
        return mcpServerRepository.findAllByProjectIdAndIsActiveTrue(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMcpServer> getAllServers(Long projectId) {
        return mcpServerRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId);
    }

    @Override
    @Transactional
    public ProjectMcpServer toggleActive(Long serverId, Long projectId, Long userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ProjectMcpServer server = mcpServerRepository.findByIdAndProjectId(serverId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("MCP Server not found: " + serverId));

        server.setActive(!server.isActive());
        return mcpServerRepository.save(server);
    }

    @Override
    @Transactional
    public void deleteServer(Long serverId, Long projectId, Long userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ProjectMcpServer server = mcpServerRepository.findByIdAndProjectId(serverId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("MCP Server not found: " + serverId));

        mcpServerRepository.delete(server);
        log.info("Removed MCP Server [{}] from project [{}]", serverId, projectId);
    }
}