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
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;
import ru.sergalas.orchestrator.entity.enums.ProjectType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.TransportType;
import ru.sergalas.orchestrator.exception.ProjectNotFoundException;
import ru.sergalas.orchestrator.repository.*;

import java.io.File;
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
    private final McpServerRepository mcpServerRepository;
    private final AgentStepRepository agentStepRepository;
    private final ArchitectQuestionRepository architectQuestionRepository;
    private final ProjectContextRepository projectContextRepository;
    private final McpProperties mcpProperties;
    private final ObjectMapper objectMapper;
    private final AgentPromptRepository agentPromptRepository;
    private final ProjectAgentPromptRepository projectAgentPromptRepository;

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
                .status(ProjectStatus.DRAFT)
                .type(request.getType() != null ? request.getType() : ProjectType.FULLSTACK)
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

        if (request.getDefaultMcpServerNames() != null) {
            for (String serverName : request.getDefaultMcpServerNames()) {
                var dbServerOpt = mcpServerRepository != null ? mcpServerRepository.findByNameIgnoreCase(serverName) : java.util.Optional.<McpServer>empty();
                if (dbServerOpt.isPresent()) {
                    var srv = dbServerOpt.get();
                    if (!projectMcpServerRepository.existsByProjectAndMcpServer(savedProject, srv)) {
                        ProjectMcpServer mcp = ProjectMcpServer.builder()
                                .project(savedProject)
                                .mcpServer(srv)
                                .isActive(true)
                                .build();
                        projectMcpServerRepository.save(mcp);
                    }
                } else if (mcpProperties != null && mcpProperties.getDefaultServers() != null) {
                    mcpProperties.getDefaultServers().stream()
                            .filter(s -> s.getName().equalsIgnoreCase(serverName))
                            .findFirst()
                            .ifPresent(cfg -> {
                                McpServer srv = mcpServerRepository.findByNameIgnoreCase(cfg.getName())
                                        .orElseGet(() -> mcpServerRepository.save(McpServer.builder()
                                                .name(cfg.getName())
                                                .url(cfg.getUrl())
                                                .target(cfg.getTarget() != null ? cfg.getTarget() : McpTarget.COMMON)
                                                .description(cfg.getDescription())
                                                .build()));
                                if (!projectMcpServerRepository.existsByProjectAndMcpServer(savedProject, srv)) {
                                    ProjectMcpServer mcp = ProjectMcpServer.builder()
                                             .project(savedProject)
                                             .mcpServer(srv)
                                             .isActive(true)
                                             .build();
                                    projectMcpServerRepository.save(mcp);
                                }
                            });
                }
            }
        }

        // Connect MCP Servers by ID if chosen
        if (request.getMcpServerIds() != null && mcpServerRepository != null) {
            for (Long srvId : request.getMcpServerIds()) {
                mcpServerRepository.findById(srvId).ifPresent(srv -> {
                    if (!projectMcpServerRepository.existsByProjectAndMcpServer(savedProject, srv)) {
                        ProjectMcpServer mcp = ProjectMcpServer.builder()
                                .project(savedProject)
                                .mcpServer(srv)
                                .isActive(true)
                                .build();
                        projectMcpServerRepository.save(mcp);
                    }
                });
            }
        }

        // Connect Agent Prompts & their MCP Servers
        if (agentPromptRepository != null && projectAgentPromptRepository != null) {
            List<StepName> stepsToConfigure = List.of(
                    StepName.ARCHITECT,
                    StepName.BACKEND_ANALYST,
                    StepName.FRONTEND_ANALYST,
                    StepName.BACKEND_DEVELOPER,
                    StepName.FRONTEND_DEVELOPER,
                    StepName.TESTER,
                    StepName.HELPER
            );

            for (StepName step : stepsToConfigure) {
                AgentPrompt selectedPrompt = null;
                if (request.getPromptIds() != null && request.getPromptIds().get(step) != null) {
                    Long promptId = request.getPromptIds().get(step);
                    selectedPrompt = agentPromptRepository.findById(promptId).orElse(null);
                }
                if (selectedPrompt == null) {
                    selectedPrompt = agentPromptRepository.findFirstByStepNameAndIsDefaultTrue(step).orElse(null);
                }
                if (selectedPrompt != null) {
                    ProjectAgentPrompt pap = ProjectAgentPrompt.builder()
                            .project(savedProject)
                            .stepName(step)
                            .agentPrompt(selectedPrompt)
                            .build();
                    projectAgentPromptRepository.save(pap);

                    if (selectedPrompt.getMcpServers() != null && projectMcpServerRepository != null) {
                        for (McpServer srv : selectedPrompt.getMcpServers()) {
                            if (!projectMcpServerRepository.existsByProjectAndMcpServer(savedProject, srv)) {
                                ProjectMcpServer mcp = ProjectMcpServer.builder()
                                        .project(savedProject)
                                        .mcpServer(srv)
                                        .isActive(true)
                                        .build();
                                projectMcpServerRepository.save(mcp);
                            }
                        }
                    }
                }
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
    public void updateStatus(Long projectId, ProjectStatus status) {
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

    @Override
    @Transactional
    public void resetProject(Long id) {
        Project project = getProjectById(id);
        log.info("Resetting project ID: {} ({})", id, project.getName());

        // 1. Delete agent steps
        agentStepRepository.deleteAllByProject(project);

        // 2. Delete architect questions
        architectQuestionRepository.deleteAllByProject(project);

        // 3. Delete all project contexts except original TASK.md
        projectContextRepository.deleteAllByProjectAndFileTypeNot(project, FileType.TASK);

        // 4. Delete physical zip archive if present
        if (project.getArchivePath() != null) {
            try {
                File archiveFile = new File(project.getArchivePath());
                if (archiveFile.exists()) {
                    archiveFile.delete();
                }
            } catch (Exception e) {
                log.warn("Failed to delete physical archive file: {}", e.getMessage());
            }
            project.setArchivePath(null);
        }

        // 5. Reset status to DRAFT
        project.setStatus(ProjectStatus.DRAFT);
        projectRepository.save(project);
        log.info("Project ID: {} successfully reset to DRAFT state", id);
    }

    @Override
    @Transactional
    public void resetDevelopment(Long id) {
        resetFromStep(id, "BACKEND_DEVELOPER");
    }

    @Override
    @Transactional
    public void resetFromStep(Long id, String stepName) {
        Project project = getProjectById(id);
        String step = (stepName == null || stepName.isBlank()) ? "ARCHITECT" : stepName.trim().toUpperCase();
        log.info("Resetting project ID: {} from step: {}", id, step);

        // 1. Очищаем физический архив, если он есть
        if (project.getArchivePath() != null) {
            try {
                File archiveFile = new File(project.getArchivePath());
                if (archiveFile.exists()) {
                    archiveFile.delete();
                }
            } catch (Exception e) {
                log.warn("Failed to delete physical archive file: {}", e.getMessage());
            }
            project.setArchivePath(null);
        }

        // 2. Сбрасываем шаги и контексты в зависимости от точки входа
        switch (step) {
            case "ARCHITECT" -> {
                agentStepRepository.deleteAllByProjectAndStepNameIn(project, 
                        List.of(StepName.ARCHITECT, StepName.BACKEND_ANALYST, StepName.FRONTEND_ANALYST, 
                                StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.WORKER, StepName.TESTER, StepName.HELPER));
                architectQuestionRepository.deleteAllByProject(project);
                projectContextRepository.deleteAllByProjectAndFileType(project, FileType.SPEC);
                projectContextRepository.deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
            case "BACKEND_ANALYST" -> {
                agentStepRepository.deleteAllByProjectAndStepNameIn(project, 
                        List.of(StepName.BACKEND_ANALYST, StepName.FRONTEND_ANALYST, 
                                StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.WORKER, StepName.TESTER, StepName.HELPER));
                projectContextRepository.deleteAllByProjectAndFileNameIn(project, 
                        List.of("BACKEND_SPEC.md", "FRONTEND_SPEC.md"));
                projectContextRepository.deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
            case "FRONTEND_ANALYST" -> {
                agentStepRepository.deleteAllByProjectAndStepNameIn(project, 
                        List.of(StepName.FRONTEND_ANALYST, 
                                StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.WORKER, StepName.TESTER, StepName.HELPER));
                projectContextRepository.deleteAllByProjectAndFileNameIn(project, 
                        List.of("FRONTEND_SPEC.md"));
                projectContextRepository.deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
            case "BACKEND_DEVELOPER", "WORKER" -> {
                agentStepRepository.deleteAllByProjectAndStepNameIn(project, 
                        List.of(StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.WORKER, StepName.TESTER, StepName.HELPER));
                projectContextRepository.deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
            case "FRONTEND_DEVELOPER" -> {
                agentStepRepository.deleteAllByProjectAndStepNameIn(project, 
                        List.of(StepName.FRONTEND_DEVELOPER, StepName.TESTER, StepName.HELPER));
                projectContextRepository.deleteAllByProjectAndFileNameIn(project, 
                        List.of("GENERATED_FRONTEND_CODE.md", "GENERATED_TESTS.md", "GENERATED_INFRA.md"));
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
            case "TESTER" -> {
                agentStepRepository.deleteAllByProjectAndStepNameIn(project, 
                        List.of(StepName.TESTER, StepName.HELPER));
                projectContextRepository.deleteAllByProjectAndFileNameIn(project, 
                        List.of("GENERATED_TESTS.md", "GENERATED_INFRA.md"));
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
            case "HELPER" -> {
                agentStepRepository.deleteAllByProjectAndStepNameIn(project, 
                        List.of(StepName.HELPER));
                projectContextRepository.deleteAllByProjectAndFileNameIn(project, 
                        List.of("GENERATED_INFRA.md"));
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
            case "ARCHIVE" -> {
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
            default -> {
                log.warn("Unknown step name '{}', defaulting to ARCHITECT reset", step);
                agentStepRepository.deleteAllByProjectAndStepNameIn(project, 
                        List.of(StepName.ARCHITECT, StepName.BACKEND_ANALYST, StepName.FRONTEND_ANALYST, 
                                StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.WORKER, StepName.TESTER, StepName.HELPER));
                architectQuestionRepository.deleteAllByProject(project);
                projectContextRepository.deleteAllByProjectAndFileType(project, FileType.SPEC);
                projectContextRepository.deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
                project.setStatus(ProjectStatus.IN_PROGRESS);
            }
        }

        projectRepository.save(project);
        log.info("Project ID: {} successfully prepared for execution from step: {}", id, step);
    }
}