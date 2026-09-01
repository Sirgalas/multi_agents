package ru.sergalas.orchestrator.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sergalas.orchestrator.dto.request.CreateProjectRequest;
import ru.sergalas.orchestrator.dto.request.OrchestratorRunRequest;
import ru.sergalas.orchestrator.dto.request.UserAnswerRequest;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.dto.response.OrchestratorStatusResponse;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.model.enums.StepName;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Unit Tests: Request/Response DTO Contracts")
class OrchestratorDtoTest {

    @Test
    @DisplayName("Edge Case: OrchestratorRunRequest defaults null startFromStep to ARCHITECT")
    void orchestratorRunRequest_NullStep_ShouldDefaultToArchitect() {
        var request = new OrchestratorRunRequest(null, true);

        assertEquals(StepName.ARCHITECT, request.startFromStep());
        assertTrue(request.runFullPipeline());
    }

    @Test
    @DisplayName("OrchestratorRunRequest preserves explicit start step")
    void orchestratorRunRequest_ExplicitStep_ShouldPreserveStep() {
        var request = new OrchestratorRunRequest(StepName.TESTER, false);

        assertEquals(StepName.TESTER, request.startFromStep());
        assertFalse(request.runFullPipeline());
    }

    @Test
    @DisplayName("CreateProjectRequest instantiation")
    void createProjectRequest_ShouldHoldData() {
        var request = new CreateProjectRequest("AI System", "Description here");

        assertEquals("AI System", request.name());
        assertEquals("Description here", request.description());
    }

    @Test
    @DisplayName("UserAnswerRequest instantiation")
    void userAnswerRequest_ShouldHoldData() {
        var request = new UserAnswerRequest("Use PostgreSQL", 101L);

        assertEquals("Use PostgreSQL", request.answer());
        assertEquals(101L, request.stepId());
    }

    @Test
    @DisplayName("ProjectResponse record mapping checks")
    void projectResponse_ShouldExposeFields() {
        Instant now = Instant.now();
        var response = new ProjectResponse(1L, "Proj", "Desc", now, 4, 2);

        assertEquals(1L, response.id());
        assertEquals("Proj", response.name());
        assertEquals("Desc", response.description());
        assertEquals(now, response.createdAt());
        assertEquals(4, response.stepsCount());
        assertEquals(2, response.filesCount());
    }

    @Test
    @DisplayName("OrchestratorStatusResponse and AgentStepResponse records instantiation")
    void orchestratorStatusResponse_ShouldHoldPipelineData() {
        Instant now = Instant.now();
        var step = new AgentStepResponse(1L, StepName.ARCHITECT, "Prompt", "Response", false, now);
        var status = new OrchestratorStatusResponse(
                1L, StepName.ARCHITECT, false, true, "What DB?", List.of(step)
        );

        assertEquals(1L, status.projectId());
        assertEquals(StepName.ARCHITECT, status.currentStep());
        assertFalse(status.pipelineComplete());
        assertTrue(status.awaitingUserInput());
        assertEquals("What DB?", status.pendingQuestion());
        assertEquals(1, status.steps().size());
    }
}