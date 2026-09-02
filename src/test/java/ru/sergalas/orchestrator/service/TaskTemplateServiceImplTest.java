package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.entity.TaskTemplate;
import ru.sergalas.orchestrator.repository.TaskTemplateRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: TaskTemplateServiceImpl")
class TaskTemplateServiceImplTest {

    @Mock
    private TaskTemplateRepository taskTemplateRepository;

    @InjectMocks
    private TaskTemplateServiceImpl taskTemplateService;

    @Test
    @DisplayName("findAll returns all task templates")
    void testFindAll() {
        List<TaskTemplate> templates = List.of(
                TaskTemplate.builder().id(1L).name("Template 1").build(),
                TaskTemplate.builder().id(2L).name("Template 2").build()
        );
        when(taskTemplateRepository.findAll()).thenReturn(templates);

        List<TaskTemplate> result = taskTemplateService.findAll();

        assertThat(result).hasSize(2).isEqualTo(templates);
        verify(taskTemplateRepository).findAll();
    }

    @Test
    @DisplayName("findById returns template when found, throws IllegalArgumentException when not found")
    void testFindById() {
        TaskTemplate template = TaskTemplate.builder().id(1L).name("T1").build();
        when(taskTemplateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(taskTemplateRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(taskTemplateService.findById(1L)).isEqualTo(template);
        assertThatThrownBy(() -> taskTemplateService.findById(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task template not found with id: 2");
    }
}