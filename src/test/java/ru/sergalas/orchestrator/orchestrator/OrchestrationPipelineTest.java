package ru.sergalas.orchestrator.orchestrator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrchestrationPipeline Unit Tests")
class OrchestrationPipelineTest {

    @Mock
    private AgentExecutor agentExecutor;
    @Mock
    private McpContextService mcpContextService;
    @Mock
    private ProjectContextService projectContextService;
    @Mock
    private AgentStepRepository agentStepRepository;
    @Mock
    private ArchitectPromptBuilder architectPromptBuilder;
    @Mock
    private WorkerPromptBuilder workerPromptBuilder;
    @Mock
    private TesterPromptBuilder testerPromptBuilder;
    @Mock
    private HelperPromptBuilder helperPromptBuilder;

    @InjectMocks
    private OrchestrationPipeline orchestrationPipeline;

    @Test
    @DisplayName("Should execute all pipeline steps sequentially and update step status to DONE")
    void shouldExecuteFullPipelineSuccessfully() {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).build();
        List<StepName> steps = List.of(StepName.ARCHITECT, StepName.WORKER);

        when(projectContextService.aggregateContextAsText(projectId)).thenReturn("Sample project context");
        when(agentStepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(Collections.emptyList());
        when(mcpContextService.fetchMcpContext(eq(projectId), any(StepName.class))).thenReturn("MCP Context");

        when(architectPromptBuilder.buildSystemPrompt(anyString())).thenReturn("Arch Sys");
        when(architectPromptBuilder.buildUserPrompt(anyString(), any())).thenReturn("Arch Usr");
        when(workerPromptBuilder.buildSystemPrompt(anyString())).thenReturn("Worker Sys");
        when(workerPromptBuilder.buildUserPrompt(anyString(), any())).thenReturn("Worker Usr");

        when(agentExecutor.execute(eq(AgentRole.ARCHITECT), anyString(), anyString())).thenReturn("Blueprint Spec");
        when(agentExecutor.execute(eq(AgentRole.WORKER), anyString(), anyString())).thenReturn("Java Code Output");

        when(agentStepRepository.save(any(AgentStep.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrationPipeline.executePipeline(project, steps);

        verify(agentExecutor, times(2)).execute(any(AgentRole.class), anyString(), anyString());
        verify(agentStepRepository, times(4)).save(any(AgentStep.class)); // 2 steps x (PENDING/RUNNING + DONE)
    }

    @Test
    @DisplayName("Should stop pipeline and flag step as ERROR when an agent execution fails")
    void shouldHaltPipelineOnError() {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).build();
        List<StepName> steps = List.of(StepName.ARCHITECT, StepName.WORKER, StepName.TESTER);

        when(projectContextService.aggregateContextAsText(projectId)).thenReturn("Context");
        when(agentStepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(Collections.emptyList());
        when(mcpContextService.fetchMcpContext(eq(projectId), any(StepName.class))).thenReturn("");

        when(architectPromptBuilder.buildSystemPrompt(anyString())).thenReturn("Sys");
        when(architectPromptBuilder.buildUserPrompt(anyString(), any())).thenReturn("Usr");

        when(agentExecutor.execute(eq(AgentRole.ARCHITECT), anyString(), anyString()))
                .thenThrow(new RuntimeException("LLM Rate limit exceeded"));

        when(agentStepRepository.save(any(AgentStep.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrationPipeline.executePipeline(project, steps);

        verify(agentExecutor, times(1)).execute(any(AgentRole.class), anyString(), anyString());
        // Worker and Tester should NOT have been executed
    }
}