package ru.sergalas.orchestrator.service.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.config.properties.McpProperties;
import ru.sergalas.orchestrator.dto.request.CreateProjectRequest;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.entity.*;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.exception.ProjectNotFoundException;
import ru.sergalas.orchestrator.repository.FileStructureTemplateRepository;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.repository.TaskTemplateRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final FileStructureTemplateRepository fileStructureTemplateRepository;
    private final ProjectContextService projectContextService;
    private final ProjectMcpServerRepository projectMcpServerRepository;
    private final McpProperties mcpProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Project createProject(CreateProjectRequest request, User user) {
        TaskTemplate template = null;
        if (request.getTaskTemplateId() != null) {
            template = taskTemplateRepository.findById(request.getTaskTemplateId()).orElse(null);
        }

        Project project = Project.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .taskTemplate(template)
                .status("DRAFT")
                .build();

        Project savedProject = projectRepository.save(project);

        // Save Task markdown in ProjectContext
        if (request.getTaskContent() != null && !request.getTaskContent().isBlank()) {
            projectContextService.saveFile(
                    savedProject,
                    "TASK.md",
                    "/TASK.md",
                    request.getTaskContent(),
                    FileType.TASK,
                    1
            );
        }

        // Save Structure template if selected
        if (request.getFileStructureTemplateId() != null) {
            fileStructureTemplateRepository.findById(request.getFileStructureTemplateId()).ifPresent(fst -> {
                try {
                    String jsonTree = objectMapper.writeValueAsString(fst.getStructureTree());
                    projectContextService.saveFile(
                            savedProject,
                            "file_structure.json",
                            "/file_structure.json",
                            jsonTree,
                            FileType.FILE_STRUCTURE,
                            1
                    );
                } catch (Exception e) {
                    log.error("Failed to serialize file structure template", e);
                }
            });
        }

        // Connect Default MCP Servers if chosen
        if (request.getDefaultMcpServerNames() != null) {
            for (String serverName : request.getDefaultMcpServerNames()) {
                mcpProperties.getDefaultServers().stream()
                        .filter(s -> s.getName().equalsIgnoreCase(serverName))
                        .findFirst()
                        .ifPresent(cfg -> {
                            ProjectMcpServer mcp = ProjectMcpServer.builder()
                                    .project(savedProject)
                                    .name(cfg.getName())
                                    .serverUrl(cfg.getUrl())
                                    .transportType(cfg.getTransport())
                                    .isActive(true)
                                    .build();
                            projectMcpServerRepository.save(mcp);
                        });
            }
        }

        return savedProject;
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjectsForUser(User user) {
        return projectRepository.findAllByUserOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public void updateStatus(Long projectId, String status) {
        Project project = getProjectById(projectId);
        project.setStatus(status);
        projectRepository.save(project);
    }

    @Override
    public ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .taskTemplateName(project.getTaskTemplate() != null ? project.getTaskTemplate().getName() : "Custom")
                .archivePath(project.getArchivePath())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}