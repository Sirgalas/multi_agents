package ru.sergalas.orchestrator.service.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.agent.ArchitectService;
import ru.sergalas.orchestrator.service.agent.HelperService;
import ru.sergalas.orchestrator.service.agent.TesterService;
import ru.sergalas.orchestrator.service.agent.WorkerService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineExecutor {

    private final ArchitectService architectService;
    private final WorkerService workerService;
    private final TesterService testerService;
    private final HelperService helperService;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final ProjectContextService contextService;
    private final ProjectArchiver projectArchiver;

    @Async
    public void runPipeline(Long projectId) {
        log.info("Starting pipeline execution for project ID: {}", projectId);
        projectService.updateStatus(projectId, "IN_PROGRESS");

        try {
            // Step 1: Architect analysis
            log.info("Pipeline Step 1: ARCHITECT");
            architectService.analyzeTask(projectId);

            ArchitectQuestionsResponse pending = architectService.getPendingQuestions(projectId);
            if (pending != null && "PENDING".equalsIgnoreCase(pending.getStatus())) {
                log.info("Pipeline paused for project {} awaiting human input (HITL)", projectId);
                projectService.updateStatus(projectId, "WAITING_FOR_INPUT");
                return;
            }

            // Step 2: Worker generation
            log.info("Pipeline Step 2: WORKER");
            workerService.generateSourceCode(projectId);

            // Step 3: Tester generation
            log.info("Pipeline Step 3: TESTER");
            testerService.generateTests(projectId);

            // Step 4: Helper generation
            log.info("Pipeline Step 4: HELPER");
            helperService.generateInfrastructure(projectId);

            // Step 5: Packaging & Archive
            log.info("Pipeline Step 5: ARCHIVING");
            Project project = projectService.getProjectById(projectId);
            List<ProjectContext> contexts = contextService.getContextByProject(project);
            ProjectArchiveResponse archiveResponse = projectArchiver.createZipArchive(project, contexts);

            project.setArchivePath(archiveResponse.getArchivePath());
            project.setStatus("COMPLETED");
            projectRepository.save(project);
            log.info("Pipeline completed successfully for project ID: {}", projectId);

        } catch (Exception e) {
            log.error("Pipeline failed for project ID: {}", projectId, e);
            projectService.updateStatus(projectId, "FAILED");
        }
    }
}