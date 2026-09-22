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
import ru.sergalas.orchestrator.service.agent.impl.FrontendWorkerService;
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
class FrontendWorkerServiceImplTest {

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
    private FrontendWorkerService frontendWorkerService;

    @Test
    @DisplayName("work генерирует клиентский код в frontend/ с учетом бэкенд контрактов и сохраняет GENERATED_FRONTEND_CODE.md")
    void work_Success() {
        // Arrange
        Project project = Project.builder().id(1L).name("Fintech App").build();
        String frontendCode = "[FILE: frontend/src/App.tsx]\n```tsx\nexport default function App() { return <div>App</div>; }\n```";

        ProjectContext backendContext = ProjectContext.builder()
                .fileName("GENERATED_BACKEND_CODE.md")
                .fileType(FileType.CONTEXT_CODE)
                .fileContent("POST /api/v1/auth/login")
                .build();

        when(projectService.getProjectById(1L)).thenReturn(project);
        when(contextService.getLatestContextByType(project, FileType.TASK))
                .thenReturn(Optional.of(ProjectContext.builder().fileContent("Task details").build()));
        when(contextService.getLatestContextByType(project, FileType.SPEC))
                .thenReturn(Optional.of(ProjectContext.builder().fileContent("Spec details").build()));
        when(contextService.getContextByProject(project)).thenReturn(List.of(backendContext));
        when(mcpClientService.aggregateFrontendMcpContext(project)).thenReturn("MCP guidelines");

        when(clientFactory.createClient(StepName.FRONTEND_DEVELOPER)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(frontendCode)))));

        // Act
        frontendWorkerService.work(1L);

        // Assert
        verify(agentStepRepository).save(argThat(step ->
                step.getStepName() == StepName.FRONTEND_DEVELOPER &&
                step.getStatus() == StepStatus.COMPLETED &&
                step.getResponse().contains("App.tsx")
        ));
    }

    @Test
    @DisplayName("isNeedAgents возвращает true для FULLSTACK и FRONTEND_ONLY, false для BACKEND_ONLY")
    void isNeedAgents_ChecksProjectType() {
        Project fullstack = Project.builder().type(ru.sergalas.orchestrator.entity.enums.ProjectType.FULLSTACK).build();
        Project frontendOnly = Project.builder().type(ru.sergalas.orchestrator.entity.enums.ProjectType.FRONTEND_ONLY).build();
        Project backendOnly = Project.builder().type(ru.sergalas.orchestrator.entity.enums.ProjectType.BACKEND_ONLY).build();

        org.junit.jupiter.api.Assertions.assertTrue(frontendWorkerService.isNeedAgents(fullstack).isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(frontendWorkerService.isNeedAgents(frontendOnly).isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(frontendWorkerService.isNeedAgents(backendOnly).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(frontendWorkerService.isNeedAgents("FRONTEND_DEVELOPER").isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(frontendWorkerService.isNeedAgents("BACKEND_DEVELOPER").isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(frontendWorkerService.isNeedAgents(fullstack, "FRONTEND_DEVELOPER").isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(frontendWorkerService.isNeedAgents(backendOnly, "FRONTEND_DEVELOPER").isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(frontendWorkerService.isNeedAgents(fullstack, "BACKEND_DEVELOPER").isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(StepName.FRONTEND_DEVELOPER, frontendWorkerService.getStepName());
    }

    @Test
    @DisplayName("isCompleted проверяет наличие GENERATED_FRONTEND_CODE.md и завершенного шага в репозитории")
    void isCompleted_ChecksContextAndStep() {
        Project project = Project.builder().id(1L).build();

        ProjectContext codeContext = ProjectContext.builder()
                .fileName("GENERATED_FRONTEND_CODE.md")
                .fileType(FileType.CONTEXT_CODE)
                .fileContent("export default function App() {}")
                .build();
        when(contextService.getContextByProject(project)).thenReturn(List.of(codeContext));
        when(agentStepRepository.findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.FRONTEND_DEVELOPER))
                .thenReturn(Optional.of(ru.sergalas.orchestrator.entity.AgentStep.builder().status(StepStatus.COMPLETED).build()));

        org.junit.jupiter.api.Assertions.assertTrue(frontendWorkerService.isCompleted(project));
    }

    @Test
    @DisplayName("work пропускает генерацию кода, если фронтенд уже завершен")
    void work_WhenAlreadyCompleted_SkipsGeneration() {
        Project project = Project.builder().id(1L).build();
        ProjectContext codeContext = ProjectContext.builder()
                .fileName("GENERATED_FRONTEND_CODE.md")
                .fileType(FileType.CONTEXT_CODE)
                .fileContent("export default function App() {}")
                .build();

        when(projectService.getProjectById(1L)).thenReturn(project);
        when(contextService.getContextByProject(project)).thenReturn(List.of(codeContext));
        when(agentStepRepository.findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.FRONTEND_DEVELOPER))
                .thenReturn(Optional.of(ru.sergalas.orchestrator.entity.AgentStep.builder().status(StepStatus.COMPLETED).build()));

        frontendWorkerService.work(1L);

        org.mockito.Mockito.verifyNoInteractions(clientFactory);
    }
}
