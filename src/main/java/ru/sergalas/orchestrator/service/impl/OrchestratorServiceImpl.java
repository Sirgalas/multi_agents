package ru.sergalas.orchestrator.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.config.AiConfig;
import ru.sergalas.orchestrator.dto.request.UserAnswerRequest;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.dto.response.OrchestratorStatusResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.service.AgentStepService;
import ru.sergalas.orchestrator.service.McpServerService;
import ru.sergalas.orchestrator.service.OrchestratorService;
import ru.sergalas.orchestrator.service.ProjectContextService;
import ru.sergalas.orchestrator.service.ProjectService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorServiceImpl implements OrchestratorService {

    private final ProjectService projectService;
    private final ProjectContextService projectContextService;
    private final McpServerService mcpServerService;
    private final AgentStepService agentStepService;
    private final AiConfig aiConfig;
    private final ChatModel chatModel;

    @Override
    public OrchestratorStatusResponse runFullPipeline(Long projectId, Authentication auth) {
        Project project = projectService.getProjectEntity(projectId, auth);

        // 1. ARCHITECT Step
        AgentStepResponse architectResponse = executeArchitectStep(project, null);
        if (architectResponse.stepStatus() == StepStatus.FAILED) {
            return buildStatusResponse(projectId, StepStatus.FAILED, null);
        }

        String pendingQuestion = extractClarificationQuestion(architectResponse.response());
        if (pendingQuestion != null) {
            return buildStatusResponse(projectId, StepStatus.IN_PROGRESS, pendingQuestion);
        }

        // 2. WORKER Step
        AgentStepResponse workerResponse = executeWorkerStep(project, architectResponse.response());
        if (workerResponse.stepStatus() == StepStatus.FAILED) {
            return buildStatusResponse(projectId, StepStatus.FAILED, null);
        }

        // 3. TESTER Step
        AgentStepResponse testerResponse = executeTesterStep(project, architectResponse.response(), workerResponse.response());
        if (testerResponse.stepStatus() == StepStatus.FAILED) {
            return buildStatusResponse(projectId, StepStatus.FAILED, null);
        }

        // 4. HELPER Step
        AgentStepResponse helperResponse = executeHelperStep(project, architectResponse.response(), workerResponse.response(), testerResponse.response());
        if (helperResponse.stepStatus() == StepStatus.FAILED) {
            return buildStatusResponse(projectId, StepStatus.FAILED, null);
        }

        return buildStatusResponse(projectId, StepStatus.COMPLETED, null);
    }

    @Override
    public AgentStepResponse runStep(Long projectId, StepName step, Authentication auth) {
        Project project = projectService.getProjectEntity(projectId, auth);
        return switch (step) {
            case ARCHITECT -> executeArchitectStep(project, null);
            case WORKER -> {
                String architectResponse = agentStepService.getLatestStep(projectId, StepName.ARCHITECT)
                        .map(AgentStep::getResponse)
                        .orElseThrow(() -> new IllegalStateException("Architect step must be run before Worker"));
                yield executeWorkerStep(project, architectResponse);
            }
            case TESTER -> {
                String architectResponse = agentStepService.getLatestStep(projectId, StepName.ARCHITECT)
                        .map(AgentStep::getResponse)
                        .orElseThrow(() -> new IllegalStateException("Architect step must be run before Tester"));
                String workerResponse = agentStepService.getLatestStep(projectId, StepName.WORKER)
                        .map(AgentStep::getResponse)
                        .orElseThrow(() -> new IllegalStateException("Worker step must be run before Tester"));
                yield executeTesterStep(project, architectResponse, workerResponse);
            }
            case HELPER -> {
                String architectResponse = agentStepService.getLatestStep(projectId, StepName.ARCHITECT)
                        .map(AgentStep::getResponse).orElse("");
                String workerResponse = agentStepService.getLatestStep(projectId, StepName.WORKER)
                        .map(AgentStep::getResponse).orElse("");
                String testerResponse = agentStepService.getLatestStep(projectId, StepName.TESTER)
                        .map(AgentStep::getResponse).orElse("");
                yield executeHelperStep(project, architectResponse, workerResponse, testerResponse);
            }
        };
    }

    @Override
    public OrchestratorStatusResponse continueWithAnswer(UserAnswerRequest request, Authentication auth) {
        Project project = projectService.getProjectEntity(request.projectId(), auth);
        AgentStep architectStep = agentStepService.getById(request.stepId());

        if (architectStep.getStepName() != StepName.ARCHITECT) {
            throw new IllegalArgumentException("User clarification answer must belong to an ARCHITECT step");
        }

        // Re-execute ARCHITECT with answer included
        AgentStepResponse newArchitectResponse = executeArchitectStep(project, request.answer());
        if (newArchitectResponse.stepStatus() == StepStatus.FAILED) {
            return buildStatusResponse(project.getId(), StepStatus.FAILED, null);
        }

        String pendingQuestion = extractClarificationQuestion(newArchitectResponse.response());
        if (pendingQuestion != null) {
            return buildStatusResponse(project.getId(), StepStatus.IN_PROGRESS, pendingQuestion);
        }

        // Proceed with pipeline
        AgentStepResponse workerResponse = executeWorkerStep(project, newArchitectResponse.response());
        if (workerResponse.stepStatus() == StepStatus.FAILED) {
            return buildStatusResponse(project.getId(), StepStatus.FAILED, null);
        }

        AgentStepResponse testerResponse = executeTesterStep(project, newArchitectResponse.response(), workerResponse.response());
        if (testerResponse.stepStatus() == StepStatus.FAILED) {
            return buildStatusResponse(project.getId(), StepStatus.FAILED, null);
        }

        AgentStepResponse helperResponse = executeHelperStep(project, newArchitectResponse.response(), workerResponse.response(), testerResponse.response());
        if (helperResponse.stepStatus() == StepStatus.FAILED) {
            return buildStatusResponse(project.getId(), StepStatus.FAILED, null);
        }

        return buildStatusResponse(project.getId(), StepStatus.COMPLETED, null);
    }

    @Override
    @Transactional(readOnly = true)
    public OrchestratorStatusResponse getStatus(Long projectId, Authentication auth) {
        projectService.getProjectEntity(projectId, auth);
        List<AgentStepResponse> steps = agentStepService.getStepsByProject(projectId);

        StepStatus overallStatus = StepStatus.PENDING;
        String pendingQuestion = null;

        if (!steps.isEmpty()) {
            boolean anyFailed = steps.stream().anyMatch(s -> s.stepStatus() == StepStatus.FAILED);
            boolean anyInProgress = steps.stream().anyMatch(s -> s.stepStatus() == StepStatus.IN_PROGRESS);

            if (anyFailed) {
                overallStatus = StepStatus.FAILED;
            } else if (anyInProgress) {
                overallStatus = StepStatus.IN_PROGRESS;
            } else {
                AgentStepResponse lastStep = steps.get(steps.size() - 1);
                if (lastStep.stepName() == StepName.ARCHITECT) {
                    pendingQuestion = extractClarificationQuestion(lastStep.response());
                    overallStatus = (pendingQuestion != null) ? StepStatus.IN_PROGRESS : StepStatus.COMPLETED;
                } else if (lastStep.stepName() == StepName.HELPER && lastStep.stepStatus() == StepStatus.COMPLETED) {
                    overallStatus = StepStatus.COMPLETED;
                } else {
                    overallStatus = StepStatus.IN_PROGRESS;
                }
            }
        }

        return new OrchestratorStatusResponse(projectId, steps, overallStatus, pendingQuestion);
    }

    private AgentStepResponse executeArchitectStep(Project project, String clarificationAnswer) {
        String model = aiConfig.getModelForStep(StepName.ARCHITECT);
        String projectContext = projectContextService.buildPromptContext(project.getId());
        String mcpContext = mcpServerService.collectMcpContext(project.getId());

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are the ARCHITECT Agent. Formulate the technical specification and architectural plan.\n")
                .append("Project Name: ").append(project.getName()).append("\n")
                .append("Project Description: ").append(project.getDescription()).append("\n\n")
                .append(projectContext)
                .append(mcpContext);

        if (clarificationAnswer != null && !clarificationAnswer.isBlank()) {
            promptBuilder.append("\nUser clarified requirements with answer:\n")
                    .append(clarificationAnswer).append("\n");
        } else {
            promptBuilder.append("\nIf any critical details are missing to create the architecture, output prefix 'CLARIFICATION_NEEDED: ' followed by your questions.\n");
        }

        String promptText = promptBuilder.toString();
        AgentStep step = agentStepService.createStep(project, StepName.ARCHITECT, promptText, model);

        try {
            String aiResponse = callModel(model, promptText);
            step = agentStepService.updateStepStatus(step.getId(), StepStatus.COMPLETED, aiResponse);
        } catch (Exception ex) {
            log.error("ARCHITECT Step failed for project {}", project.getId(), ex);
            step = agentStepService.updateStepStatus(step.getId(), StepStatus.FAILED, "Error: " + ex.getMessage());
        }

        return mapToResponse(step);
    }

    private AgentStepResponse executeWorkerStep(Project project, String architectSpec) {
        String model = aiConfig.getModelForStep(StepName.WORKER);
        String projectContext = projectContextService.buildPromptContext(project.getId());
        String mcpContext = mcpServerService.collectMcpContext(project.getId());

        String promptText = """
                You are the WORKER Agent. Implement all production code strictly adhering to the architectural specification.
                Follow best practices for Java 21, Spring Boot 3.4, clean architecture, and error handling.
                
                %s
                %s
                
                === ARCHITECTURAL SPECIFICATION ===
                %s
                === END SPECIFICATION ===
                """.formatted(projectContext, mcpContext, architectSpec);

        AgentStep step = agentStepService.createStep(project, StepName.WORKER, promptText, model);

        try {
            String aiResponse = callModel(model, promptText);
            step = agentStepService.updateStepStatus(step.getId(), StepStatus.COMPLETED, aiResponse);
        } catch (Exception ex) {
            log.error("WORKER Step failed for project {}", project.getId(), ex);
            step = agentStepService.updateStepStatus(step.getId(), StepStatus.FAILED, "Error: " + ex.getMessage());
        }

        return mapToResponse(step);
    }

    private AgentStepResponse executeTesterStep(Project project, String architectSpec, String workerCode) {
        String model = aiConfig.getModelForStep(StepName.TESTER);
        String projectContext = projectContextService.buildPromptContext(project.getId());

        String promptText = """
                You are the TESTER Agent. Write comprehensive JUnit 5 and Spring Boot unit and integration tests for the implemented code.
                Ensure edge cases, security validations, and controller endpoints are covered.
                
                %s
                
                === ARCHITECT SPECIFICATION ===
                %s
                === END SPECIFICATION ===
                
                === WORKER IMPLEMENTATION ===
                %s
                === END IMPLEMENTATION ===
                """.formatted(projectContext, architectSpec, workerCode);

        AgentStep step = agentStepService.createStep(project, StepName.TESTER, promptText, model);

        try {
            String aiResponse = callModel(model, promptText);
            step = agentStepService.updateStepStatus(step.getId(), StepStatus.COMPLETED, aiResponse);
        } catch (Exception ex) {
            log.error("TESTER Step failed for project {}", project.getId(), ex);
            step = agentStepService.updateStepStatus(step.getId(), StepStatus.FAILED, "Error: " + ex.getMessage());
        }

        return mapToResponse(step);
    }

    private AgentStepResponse executeHelperStep(Project project, String architectSpec, String workerCode, String testerTests) {
        String model = aiConfig.getModelForStep(StepName.HELPER);

        String promptText = """
                You are the HELPER Agent. Generate configuration files (e.g. build.gradle, docker-compose.yml, Dockerfile, README.md, DB migrations)
                and environment instructions required to run the full application.
                
                === SPECIFICATION ===
                %s
                === WORKER CODE SUMMARY ===
                %s
                === TESTS SUMMARY ===
                %s
                """.formatted(architectSpec, workerCode, testerTests);

        AgentStep step = agentStepService.createStep(project, StepName.HELPER, promptText, model);

        try {
            String aiResponse = callModel(model, promptText);
            step = agentStepService.updateStepStatus(step.getId(), StepStatus.COMPLETED, aiResponse);
        } catch (Exception ex) {
            log.error("HELPER Step failed for project {}", project.getId(), ex);
            step = agentStepService.updateStepStatus(step.getId(), StepStatus.FAILED, "Error: " + ex.getMessage());
        }

        return mapToResponse(step);
    }

    private String callModel(String modelName, String promptContent) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(0.3)
                .build();

        Prompt prompt = new Prompt(promptContent, options);
        var chatResponse = chatModel.call(prompt);
        if (chatResponse != null && chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
            return chatResponse.getResult().getOutput().getText();
        }
        return "";
    }

    private String extractClarificationQuestion(String response) {
        if (response == null) return null;
        if (response.contains("CLARIFICATION_NEEDED:")) {
            int idx = response.indexOf("CLARIFICATION_NEEDED:");
            return response.substring(idx + "CLARIFICATION_NEEDED:".length()).trim();
        }
        return null;
    }

    private OrchestratorStatusResponse buildStatusResponse(Long projectId, StepStatus status, String pendingQuestion) {
        List<AgentStepResponse> steps = agentStepService.getStepsByProject(projectId);
        return new OrchestratorStatusResponse(projectId, steps, status, pendingQuestion);
    }

    private AgentStepResponse mapToResponse(AgentStep step) {
        return new AgentStepResponse(
                step.getId(),
                step.getStepName(),
                step.getStepStatus(),
                step.getPrompt(),
                step.getResponse(),
                step.getModelUsed(),
                step.getCreatedAt(),
                step.getCompletedAt()
        );
    }
}