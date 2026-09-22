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
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.agent.impl.HelperService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelperServiceImplTest {

    @Mock
    private AgentClientFactory clientFactory;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectContextService contextService;

    @Mock
    private AgentStepRepository agentStepRepository;

    @Mock
    private OpenAiChatModel chatModel;

    @InjectMocks
    private HelperService helperService;

    @Test
    @DisplayName("Helper генерирует инфраструктурные манифесты (Docker, Gradle) и сохраняет GENERATED_INFRA.md")
    void work_Success() {
        // Arrange
        Project project = Project.builder().id(1L).name("Infra Project").build();
        String infra = "[FILE: Dockerfile]\n```dockerfile\nFROM eclipse-temurin:21\n```";

        when(projectService.getProjectById(1L)).thenReturn(project);
        when(clientFactory.createClient(StepName.HELPER)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(infra)))));

        // Act
        helperService.work(1L);

        // Assert
        verify(agentStepRepository).save(argThat(step ->
                step.getStepName() == StepName.HELPER &&
                step.getStatus() == StepStatus.COMPLETED
        ));

        verify(contextService).saveFile(
                eq(project),
                eq("GENERATED_INFRA.md"),
                eq("/GENERATED_INFRA.md"),
                eq(infra),
                eq(FileType.CONTEXT_CODE),
                eq(1)
        );
    }

    @Test
    @DisplayName("work: если инфраструктура уже сгенерирована, повторная генерация пропускается")
    void work_WhenAlreadyCompleted_SkipsGeneration() {
        // Arrange
        Project project = Project.builder().id(1L).name("Infra Project").build();
        ProjectContext infraCtx = ProjectContext.builder()
                .fileName("GENERATED_INFRA.md")
                .fileType(FileType.CONTEXT_CODE)
                .fileContent("FROM eclipse-temurin:21")
                .build();
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(contextService.getContextByProject(project)).thenReturn(List.of(infraCtx));
        when(agentStepRepository.findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.HELPER))
                .thenReturn(Optional.of(AgentStep.builder().status(StepStatus.COMPLETED).build()));

        // Act
        helperService.work(1L);

        // Assert
        verify(clientFactory, never()).createClient(any());
    }

    @Test
    @DisplayName("isNeedAgents и getStepName возвращают корректные значения")
    void isNeedAgents_Success() {
        Project project = Project.builder().id(1L).build();
        org.junit.jupiter.api.Assertions.assertTrue(helperService.isNeedAgents(project).isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(StepName.HELPER, helperService.getStepName());
    }
}