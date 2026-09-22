package ru.sergalas.orchestrator.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private Model model;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        model = new ConcurrentModel();
    }

    @Test
    @DisplayName("Обработка ProjectNotFoundException возвращает представление error/404")
    void handleProjectNotFound() {
        ProjectNotFoundException ex = new ProjectNotFoundException("Project 404 not found");
        String view = exceptionHandler.handleProjectNotFound(ex, model);

        assertThat(view).isEqualTo("error/404");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Project 404 not found");
    }

    @Test
    @DisplayName("Обработка TemplateNotFoundException возвращает представление error/404")
    void handleTemplateNotFound() {
        TemplateNotFoundException ex = new TemplateNotFoundException("Template missing");
        String view = exceptionHandler.handleTemplateNotFound(ex, model);

        assertThat(view).isEqualTo("error/404");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Template missing");
    }

    @Test
    @DisplayName("Обработка AgentException возвращает JSON с кодом 500")
    void handleAgentException() {
        AgentException ex = new AgentException("LLM timeout");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleAgentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "LLM timeout");
    }

    @Test
    @DisplayName("Обработка непредвиденных исключений возвращает представление error/500")
    void handleGeneralException() {
        Exception ex = new RuntimeException("Unexpected NPE");
        String view = exceptionHandler.handleGeneralException(ex, model);

        assertThat(view).isEqualTo("error/500");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Unexpected NPE");
    }
}