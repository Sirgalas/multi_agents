package ru.sergalas.orchestrator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.sergalas.orchestrator.dto.request.UserAnswerRequest;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.dto.response.OrchestratorStatusResponse;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.service.OrchestratorService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrchestratorController: REST API запуска и мониторинга пайплайна")
class OrchestratorControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OrchestratorService orchestratorService;

    @InjectMocks
    private OrchestratorController orchestratorController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orchestratorController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /projects/{id}/run: запуск полного пайплайна")
    void runPipeline_Success() throws Exception {
        OrchestratorStatusResponse res = new OrchestratorStatusResponse(10L, List.of(), StepStatus.COMPLETED, null);
        when(orchestratorService.runFullPipeline(eq(10L), any())).thenReturn(res);

        mockMvc.perform(post("/projects/10/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(10L))
                .andExpect(jsonPath("$.overallStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /projects/{id}/run/{step}: запуск конкретного шага")
    void runStep_Success() throws Exception {
        AgentStepResponse res = new AgentStepResponse(1L, StepName.WORKER, StepStatus.COMPLETED, "p", "r", "model", LocalDateTime.now(), null);
        when(orchestratorService.runStep(eq(10L), eq(StepName.WORKER), any())).thenReturn(res);

        mockMvc.perform(post("/projects/10/run/WORKER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stepName").value("WORKER"))
                .andExpect(jsonPath("$.stepStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /projects/{id}/answer: передача ответа пользователя")
    void submitAnswer_Success() throws Exception {
        UserAnswerRequest req = new UserAnswerRequest(10L, 1L, "PostgreSQL 17");
        OrchestratorStatusResponse res = new OrchestratorStatusResponse(10L, List.of(), StepStatus.COMPLETED, null);

        when(orchestratorService.continueWithAnswer(any(UserAnswerRequest.class), any())).thenReturn(res);

        mockMvc.perform(post("/projects/10/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /projects/{id}/answer: несовпадение ID проекта в path и body")
    void submitAnswer_ProjectIdMismatch_ThrowsException() throws Exception {
        UserAnswerRequest req = new UserAnswerRequest(999L, 1L, "PostgreSQL 17");

        mockMvc.perform(post("/projects/10/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /projects/{id}/status: опрос текущего статуса")
    void getStatus_Success() throws Exception {
        OrchestratorStatusResponse res = new OrchestratorStatusResponse(10L, List.of(), StepStatus.IN_PROGRESS, "Clarify?");
        when(orchestratorService.getStatus(eq(10L), any())).thenReturn(res);

        mockMvc.perform(get("/projects/10/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.pendingQuestion").value("Clarify?"));
    }
}