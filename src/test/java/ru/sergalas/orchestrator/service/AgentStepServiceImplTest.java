package ru.sergalas.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.model.AgentStep;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.enums.StepName;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.impl.AgentStepServiceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: AgentStepServiceImpl & Clarification JSON Parsing")
class AgentStepServiceImplTest {

    @Mock
    private AgentStepRepository stepRepository;

    private AgentStepServiceImpl stepService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        stepService = new AgentStepServiceImpl(stepRepository, objectMapper);
    }

    @Test
    @DisplayName("recordStep: persists and returns AgentStep")
    void recordStep_ShouldSaveStep() {
        Project project = Project.builder().id(1L).build();
        when(stepRepository.save(any(AgentStep.class))).thenAnswer(inv -> {
            AgentStep s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        AgentStep step = stepService.recordStep(project, StepName.ARCHITECT, "Prompt", "Response");

        assertNotNull(step);
        assertEquals(10L, step.getId());
        assertEquals(StepName.ARCHITECT, step.getStepName());
        assertEquals("Prompt", step.getPrompt());
        assertEquals("Response", step.getResponse());
        verify(stepRepository).save(any(AgentStep.class));
    }

    @Test
    @DisplayName("getStepsForProject: correctly detects awaitingUserInput when ARCHITECT returns questions JSON")
    void getStepsForProject_ArchitectQuestions_ShouldFlagAwaitingUserInput() {
        Long projectId = 1L;
        String responseWithQuestions = """
                Вот анализ требований. Мне нужны уточнения:
                ```json
                {
                  "questions": [
                    "Какую СУБД использовать?",
                    "Какой протокол авторизации?"
                  ]
                }
                ```
                Ожидаю ответа.
                """;

        AgentStep step = AgentStep.builder()
                .id(1L)
                .stepName(StepName.ARCHITECT)
                .prompt("Analyze")
                .response(responseWithQuestions)
                .createdAt(Instant.now())
                .build();

        when(stepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(List.of(step));

        List<AgentStepResponse> responses = stepService.getStepsForProject(projectId);

        assertEquals(1, responses.size());
        assertTrue(responses.getFirst().awaitingUserInput(),
                "Step must be flagged awaitingUserInput=true when ARCHITECT returns valid questions array");
    }

    @Test
    @DisplayName("Edge Case: WORKER step returning JSON with questions should NOT be flagged as awaitingUserInput")
    void getStepsForProject_WorkerReturningQuestions_ShouldNotFlagAwaitingUserInput() {
        Long projectId = 1L;
        String workerResponse = """
                {"questions": ["Why is this in worker response?"]}
                """;

        AgentStep step = AgentStep.builder()
                .id(2L)
                .stepName(StepName.WORKER)
                .prompt("Generate code")
                .response(workerResponse)
                .createdAt(Instant.now())
                .build();

        when(stepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(List.of(step));

        List<AgentStepResponse> responses = stepService.getStepsForProject(projectId);

        assertFalse(responses.getFirst().awaitingUserInput(),
                "Only ARCHITECT step can trigger awaitingUserInput state");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"questions\": []}",                             // Empty array
            "{\"status\": \"OK\"}",                            // No questions key
            "{\"questions\": \"not an array\"}",               // Questions is a string, not array
            "Plain text without JSON",                         // Plain text
            "Malformed JSON {questions: [broken}",             // Broken JSON
            ""                                                 // Blank response
    })
    @DisplayName("Edge Cases: getStepsForProject gracefully returns awaitingUserInput=false for non-matching responses")
    void getStepsForProject_VariousNonQuestionResponses_ShouldNotFlagAwaitingInput(String responseText) {
        Long projectId = 1L;
        AgentStep step = AgentStep.builder()
                .id(3L)
                .stepName(StepName.ARCHITECT)
                .response(responseText)
                .createdAt(Instant.now())
                .build();

        when(stepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(List.of(step));

        List<AgentStepResponse> responses = stepService.getStepsForProject(projectId);

        assertFalse(responses.getFirst().awaitingUserInput());
    }

    @Test
    @DisplayName("Edge Case: getStepsForProject with null response returns awaitingUserInput=false")
    void getStepsForProject_NullResponse_ShouldNotThrow() {
        Long projectId = 1L;
        AgentStep step = AgentStep.builder()
                .id(4L)
                .stepName(StepName.ARCHITECT)
                .response(null)
                .createdAt(Instant.now())
                .build();

        when(stepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(List.of(step));

        List<AgentStepResponse> responses = stepService.getStepsForProject(projectId);

        assertFalse(responses.getFirst().awaitingUserInput());
    }

    @Test
    @DisplayName("getLatestStep & getStep: delegation to repository")
    void stepLookupMethods_ShouldDelegateToRepository() {
        AgentStep step = AgentStep.builder().id(5L).build();
        when(stepRepository.findTopByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(step));
        when(stepRepository.findById(5L)).thenReturn(Optional.of(step));

        assertTrue(stepService.getLatestStep(1L).isPresent());
        assertTrue(stepService.getStep(5L).isPresent());
    }
}