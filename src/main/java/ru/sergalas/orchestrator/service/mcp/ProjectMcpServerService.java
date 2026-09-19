package ru.sergalas.orchestrator.service.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.request.AddMcpServerRequest;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMcpServerService {

    private final ProjectMcpServerRepository projectMcpServerRepository;
    private final ProjectService projectService;

    @Transactional
    public ProjectMcpServer registerServer(AddMcpServerRequest request) {
        Project project = projectService.getProjectById(request.getProjectId());

        ProjectMcpServer server = ProjectMcpServer.builder()
                .project(project)
                .name(request.getName())
                .serverUrl(request.getServerUrl())
                .transportType(request.getTransportType())
                .target(request.getTarget() != null ? request.getTarget() : ru.sergalas.orchestrator.entity.enums.McpTarget.COMMON)
                .token(request.getToken() != null && !request.getToken().isBlank() ? request.getToken().trim() : null)
                .isActive(request.getIsActive())
                .config(request.getConfig())
                .build();

        return projectMcpServerRepository.save(server);
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