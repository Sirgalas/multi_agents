package ru.sergalas.orchestrator.service.orchestrator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.orchestrator.OrchestratorService;
import ru.sergalas.orchestrator.service.orchestrator.PipelineExecutor;
import ru.sergalas.orchestrator.service.orchestrator.ProjectArchiver;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;

@Slf4j
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
        executePipeline(projectId, "ARCHITECT");
    }

    @Override
    public void executePipeline(Long projectId, String fromStep) {
        String targetStep = (fromStep == null || fromStep.isBlank()) ? "ARCHITECT" : fromStep.trim().toUpperCase();
        log.info("Executing pipeline for project ID: {} from step: {}", projectId, targetStep);
        projectService.resetFromStep(projectId, targetStep);
        if ("ARCHITECT".equals(targetStep)) {
            pipelineExecutor.runPipeline(projectId);
        } else {
            pipelineExecutor.continueFromStep(projectId, targetStep);
        }
    }

    @Override
    public void approveSpecAndContinue(Long projectId) {
        pipelineExecutor.continueDevelopment(projectId);
    }

    @Override
    public void restartDevelopment(Long projectId) {
        projectService.resetDevelopment(projectId);
        pipelineExecutor.continueDevelopment(projectId);
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