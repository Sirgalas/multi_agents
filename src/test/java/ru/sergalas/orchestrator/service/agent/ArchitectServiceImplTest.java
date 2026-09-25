package ru.sergalas.orchestrator.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.ArchitectQuestion;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.repository.ArchitectQuestionRepository;
import ru.sergalas.orchestrator.service.agent.impl.ArchitectServiceImpl;
import ru.sergalas.orchestrator.service.mcp.McpClientService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchitectServiceImplTest {

    @Mock
    private AgentClientFactory clientFactory;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectContextService contextService;

    @Mock
    private AgentStepRepository agentStepRepository;

    @Mock
    private ArchitectQuestionRepository questionRepository;

    @Mock
    private McpClientService mcpClientService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OpenAiChatModel chatModel;

    @InjectMocks
    private ArchitectServiceImpl architectService;

    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(10L).name("Order Service").build();
    }

    private ChatResponse createChatResponse(String content) {
        Generation generation = new Generation(new AssistantMessage(content));
        return new ChatResponse(List.of(generation));
    }

    @Test
    @DisplayName("Edge Case HITL: архитектор возвращает JSON с вопросами -> статус WAITING_FOR_INPUT и сохранение ArchitectQuestion")
    void analyzeTask_WhenQuestionsGenerated_TriggersHitl() {
        // Arrange
        String jsonQuestions = "[{\"id\": \"q1\", \"question\": \"Какая СУБД требуется?\"}]";

        when(projectService.getProjectById(10L)).thenReturn(project);
        when(contextService.getLatestContextByType(project, FileType.TASK))
                .thenReturn(Optional.of(ProjectContext.builder().fileContent("Create order service").build()));
        when(mcpClientService.aggregateMcpContext(project)).thenReturn("MCP context");
        when(clientFactory.createClient(StepName.ARCHITECT)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse(jsonQuestions));

        // Act
        architectService.analyzeTask(10L);

        // Assert
        ArgumentCaptor<AgentStep> stepCaptor = ArgumentCaptor.forClass(AgentStep.class);
        verify(agentStepRepository).save(stepCaptor.capture());
        assertThat(stepCaptor.getValue().getStatus()).isEqualTo(StepStatus.WAITING_FOR_INPUT);

        ArgumentCaptor<ArchitectQuestion> questionCaptor = ArgumentCaptor.forClass(ArchitectQuestion.class);
        verify(questionRepository).save(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(questionCaptor.getValue().getQuestions()).hasSize(1);
    }

    @Test
    @DisplayName("Edge Case: архитектор сразу формирует архитектурную спецификацию -> статус COMPLETED")
    void analyzeTask_WhenSpecificationGeneratedDirectly_CompletesStep() {
        // Arrange
        String specResponse = "SPECIFICATION:\n1. Architecture: Clean Architecture\n2. Modules: order-api, order-core";

        when(projectService.getProjectById(10L)).thenReturn(project);
        when(contextService.getLatestContextByType(project, FileType.TASK)).thenReturn(Optional.empty());
        when(mcpClientService.aggregateMcpContext(project)).thenReturn("");
        when(clientFactory.createClient(StepName.ARCHITECT)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse(specResponse));

        // Act
        architectService.analyzeTask(10L);

        // Assert
        ArgumentCaptor<AgentStep> stepCaptor = ArgumentCaptor.forClass(AgentStep.class);
        verify(agentStepRepository).save(stepCaptor.capture());
        assertThat(stepCaptor.getValue().getStatus()).isEqualTo(StepStatus.COMPLETED);

        verify(contextService).saveFile(
                eq(project),
                eq("ARCHITECTURE_SPEC.md"),
                eq("/ARCHITECTURE_SPEC.md"),
                eq(specResponse),
                eq(FileType.SPEC),
                eq(1)
        );
    }

    @Test
    @DisplayName("HITL продолжение: обработка ответов на вопросы завершает шаг и сохраняет спецификацию")
    void processAnswers_Success() {
        // Arrange
        AgentStep step = AgentStep.builder().id(50L).status(StepStatus.WAITING_FOR_INPUT).build();
        ArchitectQuestion question = ArchitectQuestion.builder()
                .id(100L)
                .project(project)
                .agentStep(step)
                .status("PENDING")
                .build();

        List<Map<String, String>> answers = List.of(Map.of("id", "q1", "answer", "PostgreSQL 17"));

        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));
        when(clientFactory.createClient(StepName.ARCHITECT)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse("SPECIFICATION:\nDB configured with PostgreSQL"));

        // Act
        architectService.processAnswers(100L, answers);

        // Assert
        assertThat(question.getStatus()).isEqualTo("ANSWERED");
        assertThat(question.getAnsweredAt()).isNotNull();
        assertThat(step.getStatus()).isEqualTo(StepStatus.COMPLETED);

        verify(contextService).saveFile(
                eq(project),
                eq("ARCHITECTURE_SPEC.md"),
                eq("/ARCHITECTURE_SPEC.md"),
                contains("PostgreSQL"),
                eq(FileType.SPEC),
                eq(1)
        );
    }

    @Test
    @DisplayName("Лимит вопросов: после 4 раундов вопросов архитектор принудительно формирует спецификацию без новых вопросов")
    void analyzeTask_WhenMaxQuestionRoundsReached_ForcesSpecificationMode() {
        // Arrange
        when(projectService.getProjectById(10L)).thenReturn(project);
        when(contextService.getLatestContextByType(project, FileType.TASK))
                .thenReturn(Optional.of(ProjectContext.builder().fileContent("Create order service").build()));
        when(mcpClientService.aggregateMcpContext(project)).thenReturn("MCP context");
        // Mock 4 existing rounds
        when(questionRepository.countByProject(project)).thenReturn(4L);
        when(clientFactory.createClient(StepName.ARCHITECT)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse("SPECIFICATION:\nFinal Architecture after 4 rounds"));

        // Act
        architectService.analyzeTask(10L);

        // Assert
        verify(questionRepository, never()).save(any(ArchitectQuestion.class));
        verify(contextService).saveFile(
                eq(project),
                eq("ARCHITECTURE_SPEC.md"),
                eq("/ARCHITECTURE_SPEC.md"),
                contains("Final Architecture after 4 rounds"),
                eq(FileType.SPEC),
                eq(1)
        );
    }

    @Test
    @DisplayName("AgentsService: getStepName возвращает ARCHITECT и isNeedAgents всегда активен")
    void agentsService_ContractMethods() {
        assertThat(architectService.getStepName()).isEqualTo(StepName.ARCHITECT);
        assertThat(architectService.isNeedAgents(project)).isPresent().contains(architectService);
        assertThat(architectService.isNeedAgents("ARCHITECT")).isPresent().contains(architectService);
        assertThat(architectService.isNeedAgents("OTHER")).isEmpty();
    }

    @Test
    @DisplayName("AgentsService: work делегирует выполнение в analyzeTask")
    void work_DelegatesToAnalyzeTask() {
        // Arrange
        when(projectService.getProjectById(10L)).thenReturn(project);
        when(contextService.getLatestContextByType(project, FileType.TASK)).thenReturn(Optional.empty());
        when(mcpClientService.aggregateMcpContext(project)).thenReturn("");
        when(clientFactory.createClient(StepName.ARCHITECT)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse("SPECIFICATION:\nArchitecture via work"));

        // Act
        architectService.work(10L);

        // Assert
        verify(contextService).saveFile(
                eq(project),
                eq("ARCHITECTURE_SPEC.md"),
                eq("/ARCHITECTURE_SPEC.md"),
                contains("Architecture via work"),
                eq(FileType.SPEC),
                eq(1)
        );
    }
}