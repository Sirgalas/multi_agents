package ru.sergalas.orchestrator.service.agent.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.agent.AgentsService;
import ru.sergalas.orchestrator.service.orchestrator.ProjectArchiver;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiverService implements AgentsService {

    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final ProjectArchiver projectArchiver;
    private final ProjectRepository projectRepository;
    private final AgentStepRepository agentStepRepository;

    @Override
    public StepName getStepName() {
        return StepName.ARCHIVER;
    }

    @Override
    public Optional<AgentsService> isNeedAgents(Project project) {
        return Optional.of(this);
    }

    @Override
    public boolean isCompleted(Project project) {
        if (project == null) {
            return false;
        }
        boolean hasArchive = project.getArchivePath() != null && !project.getArchivePath().isBlank();
        boolean hasStep = agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.ARCHIVER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);

        return hasArchive && hasStep;
    }

    @Override
    @Transactional
    public void work(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        if (isCompleted(project)) {
            log.info("Project {} archive already created at {}. Skipping archiver.", projectId, project.getArchivePath());
            return;
        }

        log.info("Pipeline Step: ARCHIVING for project ID: {}", projectId);
        List<ProjectContext> contexts = contextService.getContextByProject(project);
        ProjectArchiveResponse archiveResponse = projectArchiver.createZipArchive(project, contexts);

        project.setArchivePath(archiveResponse.getArchivePath());
        project.setStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project);

        agentStepRepository.save(AgentStep.builder()
                .project(project)
                .stepName(StepName.ARCHIVER)
                .status(StepStatus.COMPLETED)
                .build());

        log.info("Project ID: {} successfully packaged and completed. Archive path: {}", projectId, archiveResponse.getArchivePath());
    }
}
