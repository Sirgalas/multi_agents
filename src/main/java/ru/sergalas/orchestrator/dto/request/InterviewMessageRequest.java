package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterviewMessageRequest {
    
    @NotNull(message = "ID проекта обязателен")
    private Long projectId;
    
    @NotBlank(message = "Сообщение не может быть пустым")
    private String message;
    
    private Boolean finalizeInterview = false;
}