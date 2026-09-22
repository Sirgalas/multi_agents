package ru.sergalas.orchestrator.service.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.agent.impl.FrontendAnalystService;
import ru.sergalas.orchestrator.service.mcp.McpClientService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrontendAnalystServiceImplTest {

    @Mock
    private AgentClientFactory clientFactory;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectContextService contextService;

    @Mock
    private AgentStepRepository agentStepRepository;

    @Mock
    private McpClientService mcpClientService;

    @Mock
    private OpenAiChatModel chatModel;

    @InjectMocks
    private FrontendAnalystService frontendAnalystService;

    @Test
    @DisplayName("FrontendAnalyst генерирует детальное ТЗ для фронтенда и сохраняет FRONTEND_SPEC.md")
    void analyzeFrontend_Success() {
        // Arrange
        Project project = Project.builder().id(1L).name("Fintech App").build();
        String frontendSpec = "# FRONTEND TECHNICAL SPECIFICATION\n## 1. Screen Flow\n## 2. Component Hierarchy";

        ProjectContext taskContext = ProjectContext.builder()
                .fileType(FileType.TASK)
                .fileContent("Create payment platform")
                .build();
        ProjectContext archContext = ProjectContext.builder()
                .fileName("ARCHITECTURE_SPEC.md")
                .fileType(FileType.SPEC)
                .fileContent("Modular monolith architecture")
                .build();
        ProjectContext backendSpecContext = ProjectContext.builder()
                .fileName("BACKEND_SPEC.md")
                .fileType(FileType.SPEC)
                .fileContent("POST /api/v1/auth/login")
                .build();

        when(projectService.getProjectById(1L)).thenReturn(project);
        when(contextService.getLatestContextByType(project, FileType.TASK)).thenReturn(Optional.of(taskContext));
        when(contextService.getContextByProject(project)).thenReturn(List.of(archContext, backendSpecContext));
        when(mcpClientService.aggregateFrontendMcpContext(project)).thenReturn("MCP React rules");

        when(clientFactory.createClient(StepName.FRONTEND_ANALYST)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(frontendSpec)))));

        // Act
        frontendAnalystService.work(1L);

        // Assert
        verify(agentStepRepository).save(argThat(step ->
                step.getStepName() == StepName.FRONTEND_ANALYST &&
                step.getStatus() == StepStatus.COMPLETED &&
                step.getResponse().contains("FRONTEND TECHNICAL SPECIFICATION")
        ));

        verify(contextService).saveFile(
                eq(project),
                eq("FRONTEND_SPEC.md"),
                eq("/FRONTEND_SPEC.md"),
                eq(frontendSpec),
                eq(FileType.SPEC),
                eq(1)
        );
    }

    @Test
    @DisplayName("isNeedAgents возвращает true для FULLSTACK и FRONTEND_ONLY, false для BACKEND_ONLY")
    void isNeedAgents_ChecksProjectType() {
        Project fullstack = Project.builder().type(ru.sergalas.orchestrator.entity.enums.ProjectType.FULLSTACK).build();
        Project backendOnly = Project.builder().type(ru.sergalas.orchestrator.entity.enums.ProjectType.BACKEND_ONLY).build();
        Project frontendOnly = Project.builder().type(ru.sergalas.orchestrator.entity.enums.ProjectType.FRONTEND_ONLY).build();

        org.junit.jupiter.api.Assertions.assertTrue(frontendAnalystService.isNeedAgents(fullstack).isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(frontendAnalystService.isNeedAgents(backendOnly).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(frontendAnalystService.isNeedAgents(frontendOnly).isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(frontendAnalystService.isNeedAgents("BACKEND_ANALYST").isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(frontendAnalystService.isNeedAgents("FRONTEND_ANALYST").isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(frontendAnalystService.isNeedAgents(fullstack, "BACKEND_ANALYST").isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(frontendAnalystService.isNeedAgents(fullstack, "FRONTEND_ANALYST").isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(frontendAnalystService.isNeedAgents(backendOnly, "FRONTEND_ANALYST").isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(StepName.FRONTEND_ANALYST, frontendAnalystService.getStepName());
    }

    @Test
    @DisplayName("isCompleted проверяет наличие FRONTEND_SPEC.md и завершенного шага в репозитории")
    void isCompleted_ChecksContextAndStep() {
        Project project = Project.builder().id(1L).build();

        ProjectContext specContext = ProjectContext.builder()
                .fileName("FRONTEND_SPEC.md")
                .fileContent("# Frontend Spec")
                .build();
        when(contextService.getContextByProject(project)).thenReturn(List.of(specContext));
        when(agentStepRepository.findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.FRONTEND_ANALYST))
                .thenReturn(Optional.of(ru.sergalas.orchestrator.entity.AgentStep.builder().status(StepStatus.COMPLETED).build()));

        org.junit.jupiter.api.Assertions.assertTrue(frontendAnalystService.isCompleted(project));
    }

    @Test
    @DisplayName("work пропускает анализ, если спецификация уже завершена")
    void work_WhenAlreadyCompleted_SkipsAnalysis() {
        Project project = Project.builder().id(1L).build();
        ProjectContext specContext = ProjectContext.builder()
                .fileName("FRONTEND_SPEC.md")
                .fileContent("# Frontend Spec")
                .build();

        when(projectService.getProjectById(1L)).thenReturn(project);
        when(contextService.getContextByProject(project)).thenReturn(List.of(specContext));
        when(agentStepRepository.findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.FRONTEND_ANALYST))
                .thenReturn(Optional.of(ru.sergalas.orchestrator.entity.AgentStep.builder().status(StepStatus.COMPLETED).build()));

        frontendAnalystService.work(1L);

        org.mockito.Mockito.verifyNoInteractions(clientFactory);
    }
}

