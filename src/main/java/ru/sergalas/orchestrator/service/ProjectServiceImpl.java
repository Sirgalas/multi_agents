package ru.sergalas.orchestrator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.entity.*;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.repository.TaskTemplateRepository;
import ru.sergalas.orchestrator.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final ProjectContextRepository contextRepository;
    private final ProjectMcpServerRepository mcpServerRepository;

    @Override
    public List<Project> findByUser(Long userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + id));
    }

    @Override
    @Transactional
    public Project createProject(ProjectCreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        TaskTemplate template = null;
        if (request.getTaskTemplateId() != null) {
            template = taskTemplateRepository.findById(request.getTaskTemplateId()).orElse(null);
        }

        Project project = Project.builder()
                .user(user)
                .taskTemplate(template)
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        Project savedProject = projectRepository.save(project);

        // Save Task content as initial ProjectContext
        ProjectContext taskContext = ProjectContext.builder()
                .project(savedProject)
                .fileName("TASK.md")
                .fileContent(request.getTaskContent())
                .fileType(FileType.TASK)
                .build();
        contextRepository.save(taskContext);

        // Save initial MCP servers if any
        if (request.getMcpServers() != null) {
            for (var mcpReq : request.getMcpServers()) {
                ProjectMcpServer mcpServer = ProjectMcpServer.builder()
                        .project(savedProject)
                        .name(mcpReq.getName())
                        .serverUrl(mcpReq.getServerUrl())
                        .transportType(mcpReq.getTransportType())
                        .isActive(mcpReq.getIsActive() != null ? mcpReq.getIsActive() : true)
                        .build();
                mcpServerRepository.save(mcpServer);
            }
        }

        return savedProject;
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}