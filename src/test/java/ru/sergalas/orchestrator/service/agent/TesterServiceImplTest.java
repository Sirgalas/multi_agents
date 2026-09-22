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
import ru.sergalas.orchestrator.service.agent.impl.TesterService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TesterServiceImplTest {

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
    private TesterService testerService;

    @Test
    @DisplayName("Tester генерирует Unit-тесты и сохраняет GENERATED_TESTS.md")
    void work_Success() {
        // Arrange
        Project project = Project.builder().id(1L).build();
        String generatedTests = "[FILE: src/test/java/UserServiceTest.java]\n```java\nclass UserServiceTest {}\n```";

        when(projectService.getProjectById(1L)).thenReturn(project);
        when(contextService.getLatestContextByType(project, FileType.CONTEXT_CODE))
                .thenReturn(Optional.of(ProjectContext.builder().fileContent("code").build()));
        when(clientFactory.createClient(StepName.TESTER)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(generatedTests)))));

        // Act
        testerService.work(1L);

        // Assert
        verify(agentStepRepository).save(argThat(step ->
                step.getStepName() == StepName.TESTER &&
                step.getStatus() == StepStatus.COMPLETED
        ));

        verify(contextService).saveFile(
                eq(project),
                eq("GENERATED_TESTS.md"),
                eq("/GENERATED_TESTS.md"),
                eq(generatedTests),
                eq(FileType.CONTEXT_CODE),
                eq(1)
        );
    }

    @Test
    @DisplayName("work: если тесты уже сгенерированы, повторная генерация пропускается")
    void work_WhenAlreadyCompleted_SkipsGeneration() {
        // Arrange
        Project project = Project.builder().id(1L).build();
        ProjectContext testCtx = ProjectContext.builder()
                .fileName("GENERATED_TESTS.md")
                .fileType(FileType.CONTEXT_CODE)
                .fileContent("class UserServiceTest {}")
                .build();
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(contextService.getContextByProject(project)).thenReturn(List.of(testCtx));
        when(agentStepRepository.findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.TESTER))
                .thenReturn(Optional.of(AgentStep.builder().status(StepStatus.COMPLETED).build()));

        // Act
        testerService.work(1L);

        // Assert
        verify(clientFactory, never()).createClient(any());
    }

    @Test
    @DisplayName("isNeedAgents и getStepName возвращают корректные значения")
    void isNeedAgents_Success() {
        Project project = Project.builder().id(1L).build();
        org.junit.jupiter.api.Assertions.assertTrue(testerService.isNeedAgents(project).isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(StepName.TESTER, testerService.getStepName());
    }
}