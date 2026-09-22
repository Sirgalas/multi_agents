package ru.sergalas.orchestrator.service.agent;

import org.junit.jupiter.api.BeforeEach;
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
import ru.sergalas.orchestrator.dto.response.InterviewResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.exception.AgentException;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.agent.impl.InterviewerService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewerServiceImplTest {

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
    private InterviewerService interviewerService;

    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(1L).name("E-Commerce Platform").build();
    }

    private ChatResponse createChatResponse(String content) {
        Generation generation = new Generation(new AssistantMessage(content));
        return new ChatResponse(List.of(generation));
    }

    @Test
    @DisplayName("Старт интервью: генерация приветствия и сохранение начального шага")
    void startInterview_Success() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(clientFactory.createClient(StepName.INTERVIEWER)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse("Здравствуйте! Какой функционал необходим?"));
        when(agentStepRepository.save(any(AgentStep.class))).thenAnswer(i -> {
            AgentStep s = i.getArgument(0);
            s.setId(10L);
            return s;
        });

        // Act
        InterviewResponse response = interviewerService.startInterview(1L);

        // Assert
        assertThat(response.getStepId()).isEqualTo(10L);
        assertThat(response.getQuestion()).isEqualTo("Здравствуйте! Какой функционал необходим?");
        assertThat(response.getIsFinalized()).isFalse();
    }

    @Test
    @DisplayName("Edge Case: ответ пользователя продолжается в режиме диалога (не финализирован)")
    void processAnswer_InProgress() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(agentStepRepository.findAllByProjectOrderByCreatedAtAsc(project)).thenReturn(Collections.emptyList());
        when(clientFactory.createClient(StepName.INTERVIEWER)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse("Какая база данных будет использоваться?"));
        when(agentStepRepository.save(any(AgentStep.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        InterviewResponse response = interviewerService.processAnswer(1L, "Нужна корзина и каталог товаров.");

        // Assert
        assertThat(response.getQuestion()).isEqualTo("Какая база данных будет использоваться?");
        assertThat(response.getIsFinalized()).isFalse();
        assertThat(response.getGeneratedTaskMarkdown()).isNull();
    }

    @Test
    @DisplayName("Edge Case: при получении маркера FINAL_READY интервью финализируется и формируется ТЗ")
    void processAnswer_Finalized_GeneratesTask() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(agentStepRepository.findAllByProjectOrderByCreatedAtAsc(project)).thenReturn(Collections.emptyList());
        when(clientFactory.createClient(StepName.INTERVIEWER)).thenReturn(chatModel);
        
        // Первый вызов в processAnswer, второй в finalizeInterview
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(createChatResponse("Все требования понятны! FINAL_READY"))
                .thenReturn(createChatResponse("# Техническое задание\n## Стек: Java 21"));

        when(agentStepRepository.save(any(AgentStep.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        InterviewResponse response = interviewerService.processAnswer(1L, "Все требования переданы.");

        // Assert
        assertThat(response.getIsFinalized()).isTrue();
        assertThat(response.getGeneratedTaskMarkdown()).contains("# Техническое задание");

        verify(contextService).saveFile(
                eq(project),
                eq("TASK_DRAFT.md"),
                eq("/TASK_DRAFT.md"),
                contains("# Техническое задание"),
                eq(FileType.TASK_DRAFT),
                eq(1)
        );
        verify(contextService).saveFile(
                eq(project),
                eq("TASK.md"),
                eq("/TASK.md"),
                contains("# Техническое задание"),
                eq(FileType.TASK),
                eq(1)
        );
    }

    @Test
    @DisplayName("Edge Case: при сбое LLM генерируется AgentException")
    void processAnswer_ThrowsAgentException_OnModelFailure() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(clientFactory.createClient(StepName.INTERVIEWER)).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API rate limit exceeded"));

        // Act & Assert
        assertThatThrownBy(() -> interviewerService.processAnswer(1L, "Сообщение"))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("Failed to process interview step");
    }
}