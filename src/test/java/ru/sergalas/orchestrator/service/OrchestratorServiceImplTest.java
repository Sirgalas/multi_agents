package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.request.RunOrchestrationRequest;
import ru.sergalas.orchestrator.dto.request.UserAnswerRequest;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.dto.response.OrchestrationStatusResponse;
import ru.sergalas.orchestrator.enums.StepName;
import ru.sergalas.orchestrator.enums.StepStatus;
import ru.sergalas.orchestrator.exception.ProjectNotFoundException;
import ru.sergalas.orchestrator.model.AgentStep;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.orchestrator.OrchestrationPipeline;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.impl.OrchestratorServiceImpl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrchestratorServiceImpl Unit Tests")
class OrchestratorServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AgentStepRepository agentStepRepository;
    @Mock
    private OrchestrationPipeline orchestrationPipeline;

    @InjectMocks
    private OrchestratorServiceImpl orchestratorService;

    @Test
    @DisplayName("Should successfully trigger pipeline and return IN_PROGRESS status")
    void shouldTriggerPipelineSuccessfully() {
        Long projectId = 1L;
        Long userId = 10L;
        Project project = Project.builder().id(projectId).build();
        RunOrchestrationRequest request = new RunOrchestrationRequest(projectId, List.of(StepName.ARCHITECT, StepName.WORKER));

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(agentStepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(List.of());

        OrchestrationStatusResponse response = orchestratorService.runPipeline(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.projectId()).isEqualTo(projectId);
        assertThat(response.overallStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("Should throw ProjectNotFoundException when running pipeline for non-owned project")
    void shouldThrowExceptionWhenProjectNotFoundOnPipelineRun() {
        Long projectId = 999L;
        Long userId = 10L;
        RunOrchestrationRequest request = new RunOrchestrationRequest(projectId, List.of(StepName.ARCHITECT));

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestratorService.runPipeline(request, userId))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("Should append user answer and resume remaining pipeline steps")
    void shouldSubmitUserAnswerAndResumePipeline() {
        Long stepId = 100L;
        Long projectId = 1L;
        Long userId = 10L;
        Project project = Project.builder().id(projectId).build();
        AgentStep step = AgentStep.builder()
                .id(stepId)
                .project(project)
                .stepName(StepName.ARCHITECT)
                .prompt("Architect Prompt")
                .response("Do you prefer JWT or Session?")
                .status(StepStatus.DONE)
                .createdAt(OffsetDateTime.now())
                .build();

        UserAnswerRequest req = new UserAnswerRequest(stepId, "JWT please");

        when(agentStepRepository.findById(stepId)).thenReturn(Optional.of(step));
        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(agentStepRepository.save(any(AgentStep.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentStepResponse response = orchestratorService.submitUserAnswer(req, userId);

        assertThat(response).isNotNull();
        assertThat(response.response()).contains("[USER CLARIFICATION ANSWER]:\nJWT please");
        verify(agentStepRepository).save(step);
    }

    @Test
    @DisplayName("Should get step history correctly formatted")
    void shouldGetStepHistory() {
        Long projectId = 1L;
        Long userId = 10L;
        Project project = Project.builder().id(projectId).build();
        AgentStep step = AgentStep.builder()
                .id(50L)
                .project(project)
                .stepName(StepName.WORKER)
                .prompt("Prompt text")
                .response("Generated code")
                .status(StepStatus.DONE)
                .createdAt(OffsetDateTime.now())
                .build();

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(agentStepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(List.of(step));

        List<AgentStepResponse> history = orchestratorService.getStepHistory(projectId, userId);

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().id()).isEqualTo(50L);
        assertThat(history.getFirst().stepName()).isEqualTo(StepName.WORKER);
        assertThat(history.getFirst().status()).isEqualTo("DONE");
    }
}