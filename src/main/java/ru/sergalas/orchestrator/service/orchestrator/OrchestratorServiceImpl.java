package ru.sergalas.orchestrator.service.orchestrator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrchestratorServiceImpl implements OrchestratorService {

    private final PipelineExecutor pipelineExecutor;
    private final ProjectService projectService;
    private final ProjectContextService projectContextService;
    private final ProjectArchiver projectArchiver;
    private final ProjectRepository projectRepository;

    @Override
    public void executePipeline(Long projectId) {
        pipelineExecutor.runPipeline(projectId);
    }

    @Override
    @Transactional
    public ProjectArchiveResponse archiveProject(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        List<ProjectContext> contexts = projectContextService.getContextByProject(project);
        ProjectArchiveResponse response = projectArchiver.createZipArchive(project, contexts);
        project.setArchivePath(response.getArchivePath());
        projectRepository.save(project);
        return response;
    }
}