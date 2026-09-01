package ru.sergalas.orchestrator.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sergalas.orchestrator.dto.request.ProjectContextUploadRequest;
import ru.sergalas.orchestrator.dto.response.ProjectContextResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.security.UserPrincipal;
import ru.sergalas.orchestrator.service.ProjectContextService;
import ru.sergalas.orchestrator.service.ProjectService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectContextServiceImpl implements ProjectContextService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".md", ".java", ".txt", ".env", ".yaml", ".yml");

    private final ProjectContextRepository projectContextRepository;
    private final ProjectService projectService;

    @Override
    @Transactional
    public ProjectContextResponse upload(Long projectId, ProjectContextUploadRequest request, Authentication auth) {
        Project project = projectService.getProjectEntity(projectId, auth);
        MultipartFile file = request.file();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isExtensionAllowed(originalFilename)) {
            throw new IllegalArgumentException("Invalid file type. Allowed extensions: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read file content: {}", originalFilename, e);
            throw new RuntimeException("Failed to read uploaded file", e);
        }

        ProjectContext context = ProjectContext.builder()
                .project(project)
                .fileName(originalFilename)
                .fileContent(content)
                .fileType(request.fileType())
                .build();

        ProjectContext saved = projectContextRepository.save(context);
        return new ProjectContextResponse(
                saved.getId(),
                project.getId(),
                saved.getFileName(),
                saved.getFileType(),
                saved.getFileContent(),
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectContextResponse> getByProject(Long projectId, Authentication auth) {
        projectService.getProjectEntity(projectId, auth);
        return projectContextRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(c -> new ProjectContextResponse(c.getId(), projectId, c.getFileName(), c.getFileType(), c.getFileContent(), c.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long contextId, Authentication auth) {
        ProjectContext context = projectContextRepository.findById(contextId)
                .orElseThrow(() -> new IllegalArgumentException("Context file not found: " + contextId));

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        if (principal.getRole() != Role.ROLE_ADMIN && !context.getProject().getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Access denied to delete this context file");
        }

        projectContextRepository.delete(context);
    }

    @Override
    @Transactional(readOnly = true)
    public String buildPromptContext(Long projectId) {
        List<ProjectContext> contexts = projectContextRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId);
        if (contexts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== PROJECT FILES & SPECIFICATIONS ===\n");
        for (ProjectContext ctx : contexts) {
            sb.append("\n--- File: ").append(ctx.getFileName())
              .append(" (Type: ").append(ctx.getFileType().name()).append(") ---\n")
              .append(ctx.getFileContent()).append("\n");
        }
        sb.append("=== END PROJECT FILES ===\n\n");
        return sb.toString();
    }

    private boolean isExtensionAllowed(String filename) {
        String lower = filename.toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}