package ru.sergalas.orchestrator.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.ProjectContext;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.ContextBuilderService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContextBuilderServiceImpl implements ContextBuilderService {

    private final ProjectContextRepository contextRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public String buildContextString(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Проект не найден: ID " + projectId));

        StringBuilder sb = new StringBuilder();
        sb.append("=== PROJECT METADATA ===\n");
        sb.append("Project Name: ").append(project.getName()).append("\n");
        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            sb.append("Project Description: ").append(project.getDescription()).append("\n");
        }
        sb.append("========================\n\n");

        List<ProjectContext> contexts = contextRepository.findAllByProjectIdOrderByIdAsc(projectId);
        if (contexts.isEmpty()) {
            sb.append("Контекстные файлы отсутствуют. Используется только базовое описание проекта.\n");
            return sb.toString();
        }

        sb.append("=== PROJECT CONTEXT FILES ===\n");
        for (ProjectContext ctx : contexts) {
            sb.append(String.format("--- FILE: %s [TYPE: %s] ---\n", ctx.getFileName(), ctx.getFileType().name()));
            sb.append(ctx.getFileContent()).append("\n");
            sb.append("--- END FILE ---\n\n");
        }
        sb.append("=============================\n");

        return sb.toString();
    }
}