package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import ru.sergalas.orchestrator.config.AiConfig;
import ru.sergalas.orchestrator.dto.request.UserAnswerRequest;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.dto.response.OrchestratorStatusResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.security.UserPrincipal;
import ru.sergalas.orchestrator.service.impl.OrchestratorServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrchestratorService: Пайплайн оркестрации мультиагентной цепочки")
class OrchestratorServiceTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectContextService projectContextService;

    @Mock
    private McpServerService mcpServerService;

    @Mock
    private AgentStepService agentStepService;

    @Mock
    private AiConfig aiConfig;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatModel chatModel;

    @InjectMocks
    private OrchestratorServiceImpl orchestratorService;

    private Project project;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).username("dev").role(Role.ROLE_USER).build();
        auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null);
        project = Project.builder().id(100L).name("Pipeline Project").description("Desc").user(user).build();

        when(aiConfig.getModelForStep(any(StepName.class))).thenReturn("cc/claude-sonnet-4-6");
    }

    private void mockStepLifecycle(Long stepId, StepName name) {
        AgentStep step = AgentStep.builder().id(stepId).stepName(name).stepStatus(StepStatus.IN_PROGRESS).build();
        when(agentStepService.createStep(eq(project), eq(name), anyString(), anyString())).thenReturn(step);
        when(agentStepService.updateStepStatus(eq(stepId), any(StepStatus.class), anyString())).thenAnswer(inv -> {
            StepStatus st = inv.getArgument(1);
            String resp = inv.getArgument(2);
            return AgentStep.builder().id(stepId).stepName(name).stepStatus(st).response(resp).build();
        });
    }

    @Nested
    @DisplayName("Полный прогон пайплайна (runFullPipeline)")
    class RunFullPipelineTests {

        @Test
        @DisplayName("Штатный проход всех 4 агентов завершается статусом COMPLETED")
        void runFullPipeline_AllStepsSucceed_Completes() {
            when(projectService.getProjectEntity(100L, auth)).thenReturn(project);
            when(projectContextService.buildPromptContext(100L)).thenReturn("files context");
            when(mcpServerService.collectMcpContext(100L)).thenReturn("mcp context");

            mockStepLifecycle(1L, StepName.ARCHITECT);
            mockStepLifecycle(2L, StepName.WORKER);
            mockStepLifecycle(3L, StepName.TESTER);
            mockStepLifecycle(4L, StepName.HELPER);

            when(chatModel.call(any(Prompt.class)).getResult().getOutput().getText())
                    .thenReturn("Architecture Spec", "Worker Code", "Tester Tests", "Helper Configs");

            OrchestratorStatusResponse status = orchestratorService.runFullPipeline(100L, auth);

            assertThat(status.overallStatus()).isEqualTo(StepStatus.COMPLETED);
            assertThat(status.pendingQuestion()).isNull();
            verify(chatModel, times(4)).call(any(Prompt.class));
        }

        @Test
        @DisplayName("Архитектор требует уточнения -> пайплайн останавливается с pendingQuestion")
        void runFullPipeline_ArchitectRequestsClarification_Halts() {
            when(projectService.getProjectEntity(100L, auth)).thenReturn(project);
            mockStepLifecycle(1L, StepName.ARCHITECT);

            when(chatModel.call(any(Prompt.class)).getResult().getOutput().getText())
                    .thenReturn("CLARIFICATION_NEEDED: What database should be used?");

            OrchestratorStatusResponse status = orchestratorService.runFullPipeline(100L, auth);

            assertThat(status.overallStatus()).isEqualTo(StepStatus.IN_PROGRESS);
            assertThat(status.pendingQuestion()).isEqualTo("What database should be used?");
            verify(chatModel, times(1)).call(any(Prompt.class)); // Worker, Tester, Helper не вызывались
        }

        @Test
        @DisplayName("Сбой вызова AI на шаге Worker переводит статус в FAILED")
        void runFullPipeline_WorkerFails_ReturnsFailed() {
            when(projectService.getProjectEntity(100L, auth)).thenReturn(project);
            mockStepLifecycle(1L, StepName.ARCHITECT);
            mockStepLifecycle(2L, StepName.WORKER);

            when(chatModel.call(any(Prompt.class)).getResult().getOutput().getText())
                    .thenReturn("Arch Spec")
                    .thenThrow(new RuntimeException("LLM Rate Limit"));

            OrchestratorStatusResponse status = orchestratorService.runFullPipeline(100L, auth);

            assertThat(status.overallStatus()).isEqualTo(StepStatus.FAILED);
            verify(chatModel, times(2)).call(any(Prompt.class));
        }
    }

    @Nested
    @DisplayName("Запуск единичного шага (runStep)")
    class RunStepTests {

        @Test
        @DisplayName("Попытка запуска Worker без предшествующего Architect вызывает ошибку")
        void runStep_WorkerWithoutArchitect_ThrowsIllegalStateException() {
            when(projectService.getProjectEntity(100L, auth)).thenReturn(project);
            when(agentStepService.getLatestStep(100L, StepName.ARCHITECT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orchestratorService.runStep(100L, StepName.WORKER, auth))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Architect step must be run before Worker");
        }

        @Test
        @DisplayName("Запуск одиночного шага Architect успешен")
        void runStep_Architect_Success() {
            when(projectService.getProjectEntity(100L, auth)).thenReturn(project);
            mockStepLifecycle(1L, StepName.ARCHITECT);
            when(chatModel.call(any(Prompt.class)).getResult().getOutput().getText()).thenReturn("Arch Design");

            AgentStepResponse response = orchestratorService.runStep(100L, StepName.ARCHITECT, auth);

            assertThat(response.stepName()).isEqualTo(StepName.ARCHITECT);
            assertThat(response.stepStatus()).isEqualTo(StepStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Продолжение пайплайна с ответом (continueWithAnswer)")
    class ContinueWithAnswerTests {

        @Test
        @DisplayName("Ответ передается на вход Architect и пайплайн доходит до конца")
        void continueWithAnswer_Success() {
            UserAnswerRequest request = new UserAnswerRequest(100L, 1L, "Use PostgreSQL 17");
            when(projectService.getProjectEntity(100L, auth)).thenReturn(project);

            AgentStep archStep = AgentStep.builder().id(1L).stepName(StepName.ARCHITECT).build();
            when(agentStepService.getById(1L)).thenReturn(archStep);

            mockStepLifecycle(2L, StepName.ARCHITECT);
            mockStepLifecycle(3L, StepName.WORKER);
            mockStepLifecycle(4L, StepName.TESTER);
            mockStepLifecycle(5L, StepName.HELPER);

            when(chatModel.call(any(Prompt.class)).getResult().getOutput().getText())
                    .thenReturn("Updated Arch with Postgres", "Worker Code", "Tester Tests", "Helper Configs");

            OrchestratorStatusResponse response = orchestratorService.continueWithAnswer(request, auth);

            assertThat(response.overallStatus()).isEqualTo(StepStatus.COMPLETED);
            verify(chatModel, times(4)).call(any(Prompt.class));
        }

        @Test
        @DisplayName("Ответ на не-архитектурный шаг вызывает IllegalArgumentException")
        void continueWithAnswer_NonArchitectStep_ThrowsException() {
            UserAnswerRequest request = new UserAnswerRequest(100L, 5L, "Answer");
            when(projectService.getProjectEntity(100L, auth)).thenReturn(project);

            AgentStep workerStep = AgentStep.builder().id(5L).stepName(StepName.WORKER).build();
            when(agentStepService.getById(5L)).thenReturn(workerStep);

            assertThatThrownBy(() -> orchestratorService.continueWithAnswer(request, auth))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must belong to an ARCHITECT step");
        }
    }

    @Nested
    @DisplayName("Получение общего статуса (getStatus)")
    class GetStatusTests {

        @Test
        @DisplayName("При наличии шага с FAILED возвращается статус FAILED")
        void getStatus_StepFailed_ReturnsFailed() {
            when(projectService.getProjectEntity(100L, auth)).thenReturn(project);
            AgentStepResponse s1 = new AgentStepResponse(1L, StepName.ARCHITECT, StepStatus.COMPLETED, null, "ok", "model", LocalDateTime.now(), null);
            AgentStepResponse s2 = new AgentStepResponse(2L, StepName.WORKER, StepStatus.FAILED, null, "err", "model", LocalDateTime.now(), null);
            when(agentStepService.getStepsByProject(100L)).thenReturn(List.of(s1, s2));

            OrchestratorStatusResponse status = orchestratorService.getStatus(100L, auth);

            assertThat(status.overallStatus()).isEqualTo(StepStatus.FAILED);
        }
    }
}