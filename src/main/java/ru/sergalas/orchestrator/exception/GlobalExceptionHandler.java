package ru.sergalas.orchestrator.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isJsonRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        return (acceptHeader != null && acceptHeader.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || request.getRequestURI().contains("/orchestrate")
                || request.getRequestURI().contains("/answer")
                || request.getRequestURI().contains("/steps");
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public Object handleProjectNotFound(ProjectNotFoundException ex, HttpServletRequest request, Model model) {
        log.warn("Project not found: {}", ex.getMessage());
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Project Not Found",
                    "message", ex.getMessage(),
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public Object handleUnauthorized(UnauthorizedAccessException ex, HttpServletRequest request, Model model) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Access Denied",
                    "message", ex.getMessage(),
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Not Found",
                    "message", ex.getMessage(),
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return Map.of(
                "error", "Validation Failed",
                "details", errors,
                "timestamp", OffsetDateTime.now().toString()
        );
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled exception caught", ex);
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Internal Server Error",
                    "message", ex.getMessage() != null ? ex.getMessage() : "Unexpected error occurred",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }
        model.addAttribute("errorMessage", "An unexpected error occurred. Please contact the administrator.");
        return "error/500";
    }
}