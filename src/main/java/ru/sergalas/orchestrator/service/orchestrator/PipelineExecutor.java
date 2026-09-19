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
import ru.sergalas.orchestrator.service.agent.ArchitectService;
import ru.sergalas.orchestrator.service.agent.BackendAnalystService;
import ru.sergalas.orchestrator.service.agent.FrontendAnalystService;
import ru.sergalas.orchestrator.service.agent.BackendWorkerService;
import ru.sergalas.orchestrator.service.agent.FrontendWorkerService;
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
    private final BackendAnalystService backendAnalystService;
    private final FrontendAnalystService frontendAnalystService;
    private final BackendWorkerService backendWorkerService;
    private final FrontendWorkerService frontendWorkerService;
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

            // Step 1b: Backend Analyst specification
            if (!isBackendAnalystStepCompleted(project)) {
                log.info("Pipeline Step 1b: BACKEND_ANALYST - detailing backend & API specification...");
                backendAnalystService.analyzeBackend(projectId);
            }

            // Step 1c: Frontend Analyst specification
            if (!isFrontendAnalystStepCompleted(project)) {
                log.info("Pipeline Step 1c: FRONTEND_ANALYST - detailing frontend & UI specification...");
                frontendAnalystService.analyzeFrontend(projectId);
            }

            // If specifications are ready, pause for user review and approval before starting development!
            log.info("Pipeline Step 1 complete (Architect + Backend Analyst + Frontend Analyst). Specifications ready for project {}. Pausing in WAITING_FOR_INPUT for user approval.", projectId);
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
        switch (step) {
            case "ARCHITECT" -> runPipeline(projectId);
            case "BACKEND_ANALYST" -> {
                try {
                    projectService.updateStatus(projectId, ProjectStatus.IN_PROGRESS);
                    backendAnalystService.analyzeBackend(projectId);
                    frontendAnalystService.analyzeFrontend(projectId);
                    projectService.updateStatus(projectId, ProjectStatus.WAITING_FOR_INPUT);
                } catch (Exception e) {
                    log.error("Pipeline failed during BACKEND_ANALYST step for project ID: {}", projectId, e);
                    projectService.updateStatus(projectId, ProjectStatus.FAILED);
                }
            }
            case "FRONTEND_ANALYST" -> {
                try {
                    projectService.updateStatus(projectId, ProjectStatus.IN_PROGRESS);
                    frontendAnalystService.analyzeFrontend(projectId);
                    projectService.updateStatus(projectId, ProjectStatus.WAITING_FOR_INPUT);
                } catch (Exception e) {
                    log.error("Pipeline failed during FRONTEND_ANALYST step for project ID: {}", projectId, e);
                    projectService.updateStatus(projectId, ProjectStatus.FAILED);
                }
            }
            default -> continueDevelopment(projectId);
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
            if (!isBackendAnalystStepCompleted(project)) {
                log.info("Generating BACKEND_SPEC.md before development...");
                backendAnalystService.analyzeBackend(projectId);
            }
            if (!isFrontendAnalystStepCompleted(project)) {
                log.info("Generating FRONTEND_SPEC.md before development...");
                frontendAnalystService.analyzeFrontend(projectId);
            }

            // Step 2a: Backend Worker generation
            if (isBackendStepCompleted(project)) {
                log.info("Pipeline Step 2a: BACKEND_DEVELOPER already completed for project {}. Reusing backend code.", projectId);
            } else {
                log.info("Pipeline Step 2a: BACKEND_DEVELOPER - generating backend source code...");
                backendWorkerService.generateBackendCode(projectId);
            }

            // Step 2b: Frontend Worker generation
            if (isFrontendStepCompleted(project)) {
                log.info("Pipeline Step 2b: FRONTEND_DEVELOPER already completed for project {}. Reusing frontend code.", projectId);
            } else {
                log.info("Pipeline Step 2b: FRONTEND_DEVELOPER - generating frontend source code...");
                frontendWorkerService.generateFrontendCode(projectId);
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

    public boolean isBackendAnalystStepCompleted(Project project) {
        boolean hasContext = contextService.getContextByProject(project).stream()
                .anyMatch(c -> "BACKEND_SPEC.md".equals(c.getFileName())
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        boolean hasStep = agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.BACKEND_ANALYST)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);

        return hasContext && hasStep;
    }

    public boolean isFrontendAnalystStepCompleted(Project project) {
        boolean hasContext = contextService.getContextByProject(project).stream()
                .anyMatch(c -> "FRONTEND_SPEC.md".equals(c.getFileName())
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        boolean hasStep = agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.FRONTEND_ANALYST)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);

        return hasContext && hasStep;
    }

    public boolean isBackendStepCompleted(Project project) {
        boolean hasContext = contextService.getContextByProject(project).stream()
                .anyMatch(c -> c.getFileType() == FileType.CONTEXT_CODE 
                        && ("GENERATED_BACKEND_CODE.md".equals(c.getFileName()) || "GENERATED_CODE.md".equals(c.getFileName()))
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        boolean hasStep = agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.BACKEND_DEVELOPER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false)
                || agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.WORKER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);

        return hasContext && hasStep;
    }

    public boolean isFrontendStepCompleted(Project project) {
        boolean hasContext = contextService.getContextByProject(project).stream()
                .anyMatch(c -> c.getFileType() == FileType.CONTEXT_CODE 
                        && ("GENERATED_FRONTEND_CODE.md".equals(c.getFileName()) || "GENERATED_CODE.md".equals(c.getFileName()))
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        boolean hasStep = agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.FRONTEND_DEVELOPER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false)
                || agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.WORKER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);

        return hasContext && hasStep;
    }

    public boolean isWorkerStepCompleted(Project project) {
        return isBackendStepCompleted(project) && isFrontendStepCompleted(project);
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