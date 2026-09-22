package ru.sergalas.orchestrator.service.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.request.CreateAgentPromptRequest;
import ru.sergalas.orchestrator.dto.request.UpdateAgentPromptRequest;
import ru.sergalas.orchestrator.dto.response.AgentPromptResponse;
import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectAgentPrompt;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.exception.ResourceNotFoundException;
import ru.sergalas.orchestrator.repository.AgentPromptRepository;
import ru.sergalas.orchestrator.repository.McpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectAgentPromptRepository;
import ru.sergalas.orchestrator.service.prompt.impl.AgentPromptServiceImpl;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentPromptServiceImplTest {

    @Mock
    private AgentPromptRepository promptRepository;

    @Mock
    private McpServerRepository mcpServerRepository;

    @Mock
    private ProjectAgentPromptRepository projectAgentPromptRepository;

    @InjectMocks
    private AgentPromptServiceImpl promptService;

    private AgentPrompt samplePrompt;

    @BeforeEach
    void setUp() {
        samplePrompt = AgentPrompt.builder()
                .id(1L)
                .name("Senior Architect")
                .stepName(StepName.ARCHITECT)
                .prompt("Ты — Главный Архитектор. {taskContent}")
                .isFinal(false)
                .isDefault(true)
                .description("Default architect prompt")
                .mcpServers(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("getAllPrompts возвращает список всех промптов")
    void getAllPrompts_ReturnsList() {
        when(promptRepository.findAllByOrderByStepNameAscNameAsc()).thenReturn(List.of(samplePrompt));

        List<AgentPrompt> result = promptService.getAllPrompts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Senior Architect");
    }

    @Test
    @DisplayName("getPromptsByStep возвращает промпты для заданного шага")
    void getPromptsByStep_ReturnsFilteredList() {
        when(promptRepository.findAllByStepNameOrderByIsDefaultDescNameAsc(StepName.ARCHITECT))
                .thenReturn(List.of(samplePrompt));

        List<AgentPrompt> result = promptService.getPromptsByStep(StepName.ARCHITECT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStepName()).isEqualTo(StepName.ARCHITECT);
    }

    @Test
    @DisplayName("getPromptById возвращает промпт если найден")
    void getPromptById_Found() {
        when(promptRepository.findById(1L)).thenReturn(Optional.of(samplePrompt));

        AgentPrompt result = promptService.getPromptById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getPromptById выбрасывает ResourceNotFoundException если не найден")
    void getPromptById_NotFound_ThrowsException() {
        when(promptRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promptService.getPromptById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createPrompt успешно создает промпт и привязывает MCP серверы")
    void createPrompt_Success() {
        McpServer srv = McpServer.builder().id(10L).name("Spring Docs").build();
        when(mcpServerRepository.findAllById(List.of(10L))).thenReturn(List.of(srv));
        when(promptRepository.save(any(AgentPrompt.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateAgentPromptRequest request = CreateAgentPromptRequest.builder()
                .name("New Architect")
                .stepName(StepName.ARCHITECT)
                .prompt("Prompt text")
                .isFinal(false)
                .isDefault(true)
                .mcpServerIds(List.of(10L))
                .build();

        AgentPrompt result = promptService.createPrompt(request);

        assertThat(result.getName()).isEqualTo("New Architect");
        assertThat(result.getMcpServers()).contains(srv);
        verify(promptRepository).save(any(AgentPrompt.class));
    }

    @Test
    @DisplayName("updatePrompt успешно обновляет поля промпта")
    void updatePrompt_Success() {
        when(promptRepository.findById(1L)).thenReturn(Optional.of(samplePrompt));
        when(promptRepository.save(any(AgentPrompt.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAgentPromptRequest request = UpdateAgentPromptRequest.builder()
                .name("Updated Architect")
                .stepName(StepName.ARCHITECT)
                .prompt("New text")
                .isFinal(false)
                .isDefault(false)
                .build();

        AgentPrompt result = promptService.updatePrompt(1L, request);

        assertThat(result.getName()).isEqualTo("Updated Architect");
        assertThat(result.getPrompt()).isEqualTo("New text");
    }

    @Test
    @DisplayName("deletePrompt удаляет промпт")
    void deletePrompt_Success() {
        when(promptRepository.findById(1L)).thenReturn(Optional.of(samplePrompt));

        promptService.deletePrompt(1L);

        verify(promptRepository).delete(samplePrompt);
    }

    @Test
    @DisplayName("getEffectivePrompt возвращает назначенный проекту промпт")
    void getEffectivePrompt_ProjectAssigned_ReturnsAssigned() {
        Project project = Project.builder().id(5L).build();
        ProjectAgentPrompt pap = ProjectAgentPrompt.builder()
                .project(project)
                .stepName(StepName.BACKEND_DEVELOPER)
                .agentPrompt(samplePrompt)
                .build();

        when(projectAgentPromptRepository.findByProjectAndStepName(project, StepName.BACKEND_DEVELOPER))
                .thenReturn(Optional.of(pap));

        Optional<AgentPrompt> result = promptService.getEffectivePrompt(project, StepName.BACKEND_DEVELOPER, false);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Senior Architect");
    }

    @Test
    @DisplayName("getEffectivePrompt возвращает дефолтный промпт если к проекту не привязан")
    void getEffectivePrompt_NoProjectAssigned_ReturnsDefault() {
        Project project = Project.builder().id(5L).build();
        when(projectAgentPromptRepository.findByProjectAndStepName(project, StepName.BACKEND_DEVELOPER))
                .thenReturn(Optional.empty());
        when(promptRepository.findFirstByStepNameAndIsDefaultTrue(StepName.BACKEND_DEVELOPER))
                .thenReturn(Optional.of(samplePrompt));

        Optional<AgentPrompt> result = promptService.getEffectivePrompt(project, StepName.BACKEND_DEVELOPER, false);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Senior Architect");
    }

    @Test
    @DisplayName("interpolate заменяет {placeholders} в шаблоне")
    void interpolate_ReplacesPlaceholders() {
        String template = "Rules: {mcpRules}\nTask: {taskContent}";
        Map<String, String> vars = Map.of(
                "mcpRules", "Spring Security",
                "taskContent", "Build REST API"
        );

        String result = promptService.interpolate(template, vars);

        assertThat(result).isEqualTo("Rules: Spring Security\nTask: Build REST API");
    }

    @Test
    @DisplayName("toResponse корректно маппит AgentPrompt в AgentPromptResponse")
    void toResponse_MapsCorrectly() {
        AgentPromptResponse resp = promptService.toResponse(samplePrompt);

        assertThat(resp.getId()).isEqualTo(samplePrompt.getId());
        assertThat(resp.getName()).isEqualTo(samplePrompt.getName());
        assertThat(resp.getStepName()).isEqualTo(samplePrompt.getStepName());
        assertThat(resp.getPrompt()).isEqualTo(samplePrompt.getPrompt());
    }
}
