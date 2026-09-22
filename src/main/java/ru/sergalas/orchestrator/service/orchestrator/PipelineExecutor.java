package ru.sergalas.orchestrator.service.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.agent.AgentsService;
import ru.sergalas.orchestrator.service.agent.ArchitectService;
import ru.sergalas.orchestrator.service.agent.HelperService;
import ru.sergalas.orchestrator.service.agent.TesterService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineExecutor {

    private final ArchitectService architectService;
    private final List<AgentsService> agents;
    private final TesterService testerService;
    private final HelperService helperService;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final ProjectArchiver projectArchiver;

    @Async
    public void runPipeline(Long projectId) {
        log.info("Starting / checking pipeline execution for project ID: {}", projectId);
        Project project = projectService.getProjectById(projectId);

        try {
            // Check 1: If Worker, Tester and Helper are already generated -> go straight to Archiving!
            if (isWorkerStepCompleted(project) && isTesterStepCompleted(project) && isHelperStepCompleted(project)) {
                log.info("Code, tests, and infrastructure already generated for project {}. Proceeding directly to packaging & archiving.", projectId);
                continueDevelopment(projectId);
                return;
            }

            // Check 2: If code is already completed -> continue remaining development steps
            if (isWorkerStepCompleted(project)) {
                log.info("Code already completed for project {}. Continuing development pipeline.", projectId);
                continueDevelopment(projectId);
                return;
            }

            // Step 1: Architect analysis
            projectService.updateStatus(projectId, ProjectStatus.IN_PROGRESS);
            if (!isArchitectStepCompleted(project)) {
                log.info("Pipeline Step 1: ARCHITECT - analyzing task...");
                architectService.analyzeTask(projectId);
            }

            ArchitectQuestionsResponse pending = architectService.getPendingQuestions(projectId);
            if (pending != null && "PENDING".equalsIgnoreCase(pending.getStatus())) {
                log.info("Pipeline paused for project {} awaiting human input (HITL questions)", projectId);
                projectService.updateStatus(projectId, ProjectStatus.WAITING_FOR_INPUT);
                return;
            }

            // Step 1b & 1c: Agent specifications (Backend, Frontend, etc.)
            for (AgentsService agent : getAnalystAgents()) {
                agent.isNeedAgents(project).ifPresentOrElse(
                        a -> a.work(projectId),
                        () -> log.info("Project {} does not need {}. Skipping.", projectId, agent.getStepName())
                );
            }

            // If specifications are ready, pause for user review and approval before starting development!
            log.info("Pipeline Step 1 complete (Architect + Analysts). Specifications ready for project {}. Pausing in WAITING_FOR_INPUT for user approval.", projectId);
            projectService.updateStatus(projectId, ProjectStatus.WAITING_FOR_INPUT);

        } catch (Exception e) {
            log.error("Pipeline failed for project ID: {}", projectId, e);
            projectService.updateStatus(projectId, ProjectStatus.FAILED);
        }
    }

    @Async
    public void continueFromStep(Long projectId, String startStep) {
        String step = (startStep == null || startStep.isBlank()) ? "ARCHITECT" : startStep.trim().toUpperCase();
        log.info("Continuing pipeline execution for project ID: {} from step: {}", projectId, step);
        Project project = projectService.getProjectById(projectId);

        boolean isAnalystStep = getAnalystAgents().stream()
                .anyMatch(a -> a.isNeedAgents(step).isPresent());

        if ("ARCHITECT".equalsIgnoreCase(step)) {
            runPipeline(projectId);
        } else if (isAnalystStep) {
            runAnalystStep(projectId, project, step);
        } else {
            continueDevelopment(projectId);
        }
    }

    private void runAnalystStep(Long projectId, Project project, String step) {
        try {
            projectService.updateStatus(projectId, ProjectStatus.IN_PROGRESS);
            for (AgentsService agent : getAnalystAgents()) {
                agent.isNeedAgents(project, step).ifPresent(a -> a.work(projectId));
            }
            projectService.updateStatus(projectId, ProjectStatus.WAITING_FOR_INPUT);
        } catch (Exception e) {
            log.error("Pipeline failed during {} step for project ID: {}", step, projectId, e);
            projectService.updateStatus(projectId, ProjectStatus.FAILED);
        }
    }

    @Async
    public void continueDevelopment(Long projectId) {
        log.info("Starting development steps (Backend -> Frontend -> Tester -> Helper -> Archiving) for project ID: {}", projectId);
        Project project = projectService.getProjectById(projectId);
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            projectService.updateStatus(projectId, ProjectStatus.IN_PROGRESS);
        }

        try {
            // Ensure specs are in place before writing code
            for (AgentsService agent : getAnalystAgents()) {
                agent.isNeedAgents(project).ifPresent(a -> a.work(projectId));
            }

            // Step 2: Worker generation (Backend, Frontend)
            for (AgentsService worker : getWorkerAgents()) {
                worker.isNeedAgents(project).ifPresentOrElse(
                        w -> w.work(projectId),
                        () -> log.info("Project {} does not need {}. Skipping.", projectId, worker.getStepName())
                );
            }

            // Step 3: Tester generation
            if (isTesterStepCompleted(project)) {
                log.info("Pipeline Step 3: TESTER already completed for project {}. Reusing generated tests.", projectId);
            } else {
                log.info("Pipeline Step 3: TESTER - generating unit/integration tests...");
                testerService.generateTests(projectId);
            }

            // Step 4: Helper generation
            if (isHelperStepCompleted(project)) {
                log.info("Pipeline Step 4: HELPER already completed for project {}. Reusing infrastructure & configuration.", projectId);
            } else {
                log.info("Pipeline Step 4: HELPER - generating infrastructure & configs...");
                helperService.generateInfrastructure(projectId);
            }

            // Step 5: Packaging & Archive
            log.info("Pipeline Step 5: ARCHIVING for project ID: {}", projectId);
            Project currentProject = projectService.getProjectById(projectId);
            List<ProjectContext> contexts = contextService.getContextByProject(currentProject);
            ProjectArchiveResponse archiveResponse = projectArchiver.createZipArchive(currentProject, contexts);

            currentProject.setArchivePath(archiveResponse.getArchivePath());
            currentProject.setStatus(ProjectStatus.COMPLETED);
            projectRepository.save(currentProject);
            log.info("Pipeline completed successfully for project ID: {}", projectId);

        } catch (Exception e) {
            log.error("Pipeline failed during development for project ID: {}", projectId, e);
            projectService.updateStatus(projectId, ProjectStatus.FAILED);
        }
    }

    public boolean isArchitectStepCompleted(Project project) {
        ArchitectQuestionsResponse pending = architectService.getPendingQuestions(project.getId());
        if (pending != null && "PENDING".equalsIgnoreCase(pending.getStatus())) {
            return false;
        }

        Optional<ProjectContext> specContext = contextService.getContextByProject(project).stream()
                .filter(c -> "ARCHITECTURE_SPEC.md".equals(c.getFileName()) || (c.getFileType() == FileType.SPEC && !"BACKEND_SPEC.md".equals(c.getFileName()) && !"FRONTEND_SPEC.md".equals(c.getFileName())))
                .findFirst();

        if (specContext.isEmpty()) {
            specContext = contextService.getLatestContextByType(project, FileType.SPEC)
                    .filter(c -> !"BACKEND_SPEC.md".equals(c.getFileName()) && !"FRONTEND_SPEC.md".equals(c.getFileName()));
        }

        if (specContext.isEmpty() || specContext.get().getFileContent() == null || specContext.get().getFileContent().isBlank()) {
            return false;
        }

        String content = specContext.get().getFileContent().trim();
        if (content.startsWith("[") && content.contains("\"question\":")) {
            return false;
        }

        return true;
    }


    public boolean isWorkerStepCompleted(Project project) {
        if (project == null) {
            return false;
        }
        return getWorkerAgents().stream()
                .filter(worker -> worker.isNeedAgents(project).isPresent())
                .allMatch(worker -> worker.isCompleted(project));
    }

    private List<AgentsService> getAnalystAgents() {
        if (agents == null) {
            return List.of();
        }
        return agents.stream()
                .filter(a -> a.getStepName() == StepName.BACKEND_ANALYST || a.getStepName() == StepName.FRONTEND_ANALYST || a.isAnalyst())
                .toList();
    }

    private List<AgentsService> getWorkerAgents() {
        if (agents == null) {
            return List.of();
        }
        return agents.stream()
                .filter(a -> a.getStepName() == StepName.BACKEND_DEVELOPER || a.getStepName() == StepName.FRONTEND_DEVELOPER || a.getStepName() == StepName.WORKER || a.isWorker())
                .toList();
    }

    public boolean isTesterStepCompleted(Project project) {
        boolean hasContextTests = contextService.getContextByProject(project).stream()
                .anyMatch(c -> c.getFileType() == FileType.CONTEXT_CODE && "GENERATED_TESTS.md".equals(c.getFileName())
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        return hasContextTests && agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.TESTER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);
    }

    public boolean isHelperStepCompleted(Project project) {
        boolean hasContextInfra = contextService.getContextByProject(project).stream()
                .anyMatch(c -> c.getFileType() == FileType.CONTEXT_CODE && "GENERATED_INFRA.md".equals(c.getFileName())
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        return hasContextInfra && agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.HELPER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);
    }
}