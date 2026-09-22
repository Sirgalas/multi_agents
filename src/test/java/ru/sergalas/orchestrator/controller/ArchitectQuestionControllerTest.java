package ru.sergalas.orchestrator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.sergalas.orchestrator.dto.request.ArchitectAnswerRequest;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.service.agent.ArchitectService;
import ru.sergalas.orchestrator.service.orchestrator.PipelineExecutor;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchitectQuestionController.class)
@AutoConfigureMockMvc
class ArchitectQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArchitectService architectService;

    @MockitoBean
    private PipelineExecutor pipelineExecutor;

    @Test
    @WithMockUser
    @DisplayName("GET /api/projects/{projectId}/architect/questions возвращает ожидающие вопросы")
    void getPendingQuestions_ReturnsQuestions() throws Exception {
        // Arrange
        ArchitectQuestionsResponse response = ArchitectQuestionsResponse.builder()
                .questionId(123L)
                .projectId(1L)
                .status("PENDING")
                .questions(List.of(Map.of("id", "q1", "question", "Выбор базы данных?")))
                .build();

        when(architectService.getPendingQuestions(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/projects/1/architect/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(123))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.questions[0].question").value("Выбор базы данных?"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/projects/{projectId}/architect/answers принимает ответы и возобновляет пайплайн")
    void submitAnswers_ResumesPipeline() throws Exception {
        // Arrange
        ArchitectAnswerRequest request = new ArchitectAnswerRequest();
        request.setQuestionId(123L);
        request.setAnswers(List.of(Map.of("id", "q1", "answer", "PostgreSQL 17")));

        // Act & Assert
        mockMvc.perform(post("/api/projects/1/architect/answers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(architectService).processAnswers(123L, request.getAnswers());
        verify(pipelineExecutor).runPipeline(1L);
    }
}