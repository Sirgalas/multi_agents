package ru.sergalas.orchestrator.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.ProjectContext;
import ru.sergalas.orchestrator.model.enums.FileType;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.FileStorageService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final ProjectContextRepository contextRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public ProjectContext store(Long projectId, MultipartFile file, FileType fileType) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file: " + file.getOriginalFilename());
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed_file";

            ProjectContext context = ProjectContext.builder()
                    .project(project)
                    .fileName(filename)
                    .fileContent(content)
                    .fileType(fileType != null ? fileType : FileType.TASK)
                    .build();

            ProjectContext saved = contextRepository.save(context);
            log.info("Stored file '{}' (Type: {}) for project ID={}", filename, fileType, projectId);
            return saved;
        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to process uploaded file", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectContext> listForProject(Long projectId) {
        return contextRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId);
    }

    @Override
    @Transactional
    public void delete(Long contextId, Long projectId) {
        ProjectContext context = contextRepository.findByIdAndProjectId(contextId, projectId)
                .orElseThrow(() -> new NoSuchElementException("File context not found: ID=" + contextId + ", ProjectID=" + projectId));
        contextRepository.delete(context);
        log.info("Deleted context file ID={} from project ID={}", contextId, projectId);
    }
}