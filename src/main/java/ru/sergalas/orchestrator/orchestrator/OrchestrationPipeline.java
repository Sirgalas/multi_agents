package ru.sergalas.orchestrator.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.sergalas.orchestrator.enums.StepName;
import ru.sergalas.orchestrator.enums.StepStatus;
import ru.sergalas.orchestrator.model.AgentStep;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.orchestrator.prompt.ArchitectPromptBuilder;
import ru.sergalas.orchestrator.orchestrator.prompt.HelperPromptBuilder;
import ru.sergalas.orchestrator.orchestrator.prompt.TesterPromptBuilder;
import ru.sergalas.orchestrator.orchestrator.prompt.WorkerPromptBuilder;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.McpContextService;
import ru.sergalas.orchestrator.service.ProjectContextService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrchestrationPipeline {

    private final AgentExecutor agentExecutor;
    private final McpContextService mcpContextService;
    private final ProjectContextService projectContextService;
    private final AgentStepRepository agentStepRepository;

    private final ArchitectPromptBuilder architectPromptBuilder;
    private final WorkerPromptBuilder workerPromptBuilder;
    private final TesterPromptBuilder testerPromptBuilder;
    private final HelperPromptBuilder helperPromptBuilder;

    public void executePipeline(Project project, List<StepName> stepsToExecute) {
        Long projectId = project.getId();
        String aggregatedContext = projectContextService.aggregateContextAsText(projectId);
        Map<StepName, String> stepOutputs = new HashMap<>();

        // Load existing completed steps into memory
        agentStepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId).forEach(step -> {
            if (step.getStatus() == StepStatus.DONE && step.getResponse() != null) {
                stepOutputs.put(step.getStepName(), step.getResponse());
            }
        });

        for (StepName stepName : stepsToExecute) {
            AgentRole role = AgentRole.fromStepName(stepName);
            String mcpContext = mcpContextService.fetchMcpContext(projectId, stepName);

            String systemPrompt;
            String userPrompt;

            switch (stepName) {
                case ARCHITECT -> {
                    systemPrompt = architectPromptBuilder.buildSystemPrompt(mcpContext);
                    userPrompt = architectPromptBuilder.buildUserPrompt(aggregatedContext, stepOutputs.get(StepName.ARCHITECT));
                }
                case WORKER -> {
                    systemPrompt = workerPromptBuilder.buildSystemPrompt(mcpContext);
                    userPrompt = workerPromptBuilder.buildUserPrompt(aggregatedContext, stepOutputs.get(StepName.ARCHITECT));
                }
                case TESTER -> {
                    systemPrompt = testerPromptBuilder.buildSystemPrompt(mcpContext);
                    userPrompt = testerPromptBuilder.buildUserPrompt(aggregatedContext, stepOutputs.get(StepName.WORKER));
                }
                case HELPER -> {
                    systemPrompt = helperPromptBuilder.buildSystemPrompt(mcpContext);
                    userPrompt = helperPromptBuilder.buildUserPrompt(aggregatedContext, stepOutputs.get(StepName.WORKER), stepOutputs.get(StepName.TESTER));
                }
                default -> throw new IllegalArgumentException("Unsupported step: " + stepName);
            }

            AgentStep agentStep = AgentStep.builder()
                    .project(project)
                    .stepName(stepName)
                    .prompt(userPrompt)
                    .status(StepStatus.RUNNING)
                    .build();
            agentStep = agentStepRepository.save(agentStep);

            try {
                String response = agentExecutor.execute(role, systemPrompt, userPrompt);
                agentStep.setResponse(response);
                agentStep.setStatus(StepStatus.DONE);
                agentStepRepository.save(agentStep);
                stepOutputs.put(stepName, response);
                log.info("Step [{}] completed successfully for project [{}]", stepName, projectId);
            } catch (Exception ex) {
                log.error("Step [{}] failed for project [{}]", stepName, projectId, ex);
                agentStep.setStatus(StepStatus.ERROR);
                agentStep.setResponse("Execution error: " + ex.getMessage());
                agentStepRepository.save(agentStep);
                break;
            }
        }
    }
}