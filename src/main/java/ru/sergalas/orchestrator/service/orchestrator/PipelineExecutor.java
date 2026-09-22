package ru.sergalas.orchestrator.service.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.service.agent.AgentsService;
import ru.sergalas.orchestrator.service.agent.ArchitectService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineExecutor {

    private final ArchitectService architectService;
    private final List<AgentsService> agents;
    private final ProjectService projectService;

    @Async
    public void runPipeline(Long projectId) {
        log.info("Starting / checking pipeline execution for project ID: {}", projectId);
        Project project = projectService.getProjectById(projectId);

        try {
            // Step 1: Architect analysis
            projectService.updateStatus(projectId, ProjectStatus.IN_PROGRESS);
            if (!architectService.isCompleted(project)) {
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

            // Development and Archiving agents (Backend, Frontend, Tester, Helper, Archiver)
            for (AgentsService devAgent : getDevelopmentAgents()) {
                devAgent.isNeedAgents(project).ifPresentOrElse(
                        a -> a.work(projectId),
                        () -> log.info("Project {} does not need {}. Skipping.", projectId, devAgent.getStepName())
                );
            }

            log.info("Development pipeline execution completed for project ID: {}", projectId);

        } catch (Exception e) {
            log.error("Pipeline failed during development for project ID: {}", projectId, e);
            projectService.updateStatus(projectId, ProjectStatus.FAILED);
        }
    }

    private List<AgentsService> getAnalystAgents() {
        if (agents == null) {
            return List.of();
        }
        return agents.stream()
                .filter(AgentsService::isAnalyst)
                .toList();
    }

    public List<AgentsService> getDevelopmentAgents() {
        if (agents == null) {
            return List.of();
        }
        return agents.stream()
                .filter(a -> a.isDeveloper() || a.getStepName() == StepName.TESTER || a.getStepName() == StepName.HELPER || a.getStepName() == StepName.ARCHIVER)
                .toList();
    }
}