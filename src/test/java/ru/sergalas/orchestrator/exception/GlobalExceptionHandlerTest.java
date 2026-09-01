package ru.sergalas.orchestrator.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;
    private Model model;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        model = new ConcurrentModel();
    }

    @Test
    @DisplayName("Should return 404 JSON for ProjectNotFoundException when JSON request is made")
    void shouldReturnJsonForProjectNotFound() {
        when(request.getHeader("Accept")).thenReturn("application/json");
        when(request.getRequestURI()).thenReturn("/projects/10/orchestrate");

        ProjectNotFoundException ex = new ProjectNotFoundException(10L);
        Object response = exceptionHandler.handleProjectNotFound(ex, request, model);

        assertThat(response).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) response;
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 404 HTML view name for ProjectNotFoundException when browser request is made")
    void shouldReturnHtmlViewForProjectNotFound() {
        when(request.getHeader("Accept")).thenReturn("text/html");
        when(request.getRequestURI()).thenReturn("/projects/10");

        ProjectNotFoundException ex = new ProjectNotFoundException(10L);
        Object viewName = exceptionHandler.handleProjectNotFound(ex, request, model);

        assertThat(viewName).isEqualTo("error/404");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Project with id 10 not found");
    }

    @Test
    @DisplayName("Should return 403 JSON for UnauthorizedAccessException when AJAX request is made")
    void shouldReturnJsonForUnauthorizedAccess() {
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        when(request.getRequestURI()).thenReturn("/projects/10/delete");

        UnauthorizedAccessException ex = new UnauthorizedAccessException("Forbidden action");
        Object response = exceptionHandler.handleUnauthorized(ex, request, model);

        assertThat(response).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) response;
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should format validation errors map for MethodArgumentNotValidException")
    void shouldHandleValidationExceptions() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("createProjectRequest", "name", "Project name is required")
        ));

        Map<String, Object> body = exceptionHandler.handleValidationExceptions(ex);

        assertThat(body).containsKey("details");
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) body.get("details");
        assertThat(details).containsEntry("name", "Project name is required");
    }

    @Test
    @DisplayName("Should return 500 internal server error for unhandled exceptions")
    void shouldHandleGenericException() {
        when(request.getHeader("Accept")).thenReturn("application/json");
        when(request.getRequestURI()).thenReturn("/api/unknown");

        Exception ex = new RuntimeException("Database timeout");
        Object response = exceptionHandler.handleGenericException(ex, request, model);

        assertThat(response).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) response;
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}