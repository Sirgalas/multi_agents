package ru.sergalas.orchestrator.service.project.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.service.project.ProjectContextService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectContextServiceImpl implements ProjectContextService {

    private final ProjectContextRepository projectContextRepository;

    @Override
    @Transactional
    public ProjectContext saveFile(Project project, String fileName, String filePath, String content, FileType fileType, int iteration) {
        ProjectContext context = projectContextRepository.findFirstByProjectAndFileName(project, fileName)
                .map(existing -> {
                    existing.setFilePath(filePath);
                    existing.setFileContent(content);
                    existing.setFileType(fileType);
                    existing.setIteration(iteration);
                    return existing;
                })
                .orElseGet(() -> ProjectContext.builder()
                        .project(project)
                        .fileName(fileName)
                        .filePath(filePath)
                        .fileContent(content)
                        .fileType(fileType)
                        .iteration(iteration)
                        .build());
        return projectContextRepository.save(context);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectContext> getContextByProject(Project project) {
        return projectContextRepository.findAllByProjectOrderByCreatedAtAsc(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectContext> getLatestContextByType(Project project, FileType fileType) {
        return projectContextRepository.findFirstByProjectAndFileTypeOrderByIterationDesc(project, fileType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectContext> getAllByType(Project project, FileType fileType) {
        return projectContextRepository.findAllByProjectAndFileType(project, fileType);
    }
}