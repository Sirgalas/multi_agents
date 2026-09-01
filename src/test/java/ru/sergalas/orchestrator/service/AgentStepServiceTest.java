package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.impl.AgentStepServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentStepService: Жизненный цикл и хранение шагов агентов")
class AgentStepServiceTest {

    @Mock
    private AgentStepRepository agentStepRepository;

    @InjectMocks
    private AgentStepServiceImpl agentStepService;

    @Test
    @DisplayName("Создание нового шага в статусе IN_PROGRESS")
    void createStep_InitializesInProgress() {
        Project project = Project.builder().id(1L).build();

        when(agentStepRepository.save(any(AgentStep.class))).thenAnswer(inv -> {
            AgentStep s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        AgentStep step = agentStepService.createStep(project, StepName.ARCHITECT, "Prompt", "cc/claude-sonnet-4-6");

        assertThat(step.getId()).isEqualTo(10L);
        assertThat(step.getStepStatus()).isEqualTo(StepStatus.IN_PROGRESS);
        assertThat(step.getStepName()).isEqualTo(StepName.ARCHITECT);
        verify(agentStepRepository).save(any(AgentStep.class));
    }

    @Test
    @DisplayName("Обновление статуса шага на COMPLETED проставляет completedAt")
    void updateStepStatus_Completed_SetsCompletedAt() {
        AgentStep step = AgentStep.builder()
                .id(10L)
                .stepStatus(StepStatus.IN_PROGRESS)
                .build();

        when(agentStepRepository.findById(10L)).thenReturn(Optional.of(step));
        when(agentStepRepository.save(any(AgentStep.class))).thenAnswer(inv -> inv.getArgument(0));

        AgentStep updated = agentStepService.updateStepStatus(10L, StepStatus.COMPLETED, "Response code");

        assertThat(updated.getStepStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(updated.getResponse()).isEqualTo("Response code");
        assertThat(updated.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Получение списка шагов проекта")
    void getStepsByProject_ReturnsMappedList() {
        AgentStep s1 = AgentStep.builder().id(1L).stepName(StepName.ARCHITECT).stepStatus(StepStatus.COMPLETED).build();
        AgentStep s2 = AgentStep.builder().id(2L).stepName(StepName.WORKER).stepStatus(StepStatus.IN_PROGRESS).build();

        when(agentStepRepository.findAllByProjectIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(s1, s2));

        List<AgentStepResponse> steps = agentStepService.getStepsByProject(10L);

        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).stepName()).isEqualTo(StepName.ARCHITECT);
        assertThat(steps.get(1).stepName()).isEqualTo(StepName.WORKER);
    }

    @Test
    @DisplayName("Поиск несуществующего шага выбрасывает исключение")
    void getById_NotFound_ThrowsIllegalArgumentException() {
        when(agentStepRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentStepService.getById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent step not found: 999");
    }
}