package ru.sergalas.orchestrator.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.dto.response.McpServerResponse;
import ru.sergalas.orchestrator.dto.response.ProjectContextResponse;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.repository.UserRepository;
import ru.sergalas.orchestrator.security.UserPrincipal;
import ru.sergalas.orchestrator.service.ProjectService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsForUser(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        List<Project> projects;

        if (principal.getRole() == Role.ROLE_ADMIN) {
            projects = projectRepository.findAll();
        } else {
            projects = projectRepository.findAllByUserIdOrderByCreatedAtDesc(principal.getId());
        }

        return projects.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, Authentication auth) {
        Project project = getProjectEntity(projectId, auth);
        return mapToResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectEntity(Long projectId, Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + projectId));

        if (principal.getRole() != Role.ROLE_ADMIN && !project.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have permission to access this project");
        }

        return project;
    }

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request, Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getUsername()));

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .user(user)
                .build();

        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Authentication auth) {
        Project project = getProjectEntity(projectId, auth);
        projectRepository.delete(project);
    }

    private ProjectResponse mapToResponse(Project project) {
        List<ProjectContextResponse> contextResponses = project.getContexts().stream()
                .map(c -> new ProjectContextResponse(c.getId(), project.getId(), c.getFileName(), c.getFileType(), c.getFileContent(), c.getCreatedAt()))
                .toList();

        List<McpServerResponse> mcpResponses = project.getMcpServers().stream()
                .map(m -> new McpServerResponse(m.getId(), m.getName(), m.getServerUrl(), m.getTransportType(), m.isActive(), m.getCreatedAt()))
                .toList();

        List<AgentStepResponse> stepResponses = project.getSteps().stream()
                .map(s -> new AgentStepResponse(s.getId(), s.getStepName(), s.getStepStatus(), s.getPrompt(), s.getResponse(), s.getModelUsed(), s.getCreatedAt(), s.getCompletedAt()))
                .toList();

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                contextResponses,
                mcpResponses,
                stepResponses
        );
    }
}