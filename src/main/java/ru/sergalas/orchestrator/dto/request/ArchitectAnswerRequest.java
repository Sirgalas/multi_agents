package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ArchitectAnswerRequest {
    
    @NotNull(message = "ID вопроса обязателен")
    private Long questionId;
    
    @NotEmpty(message = "Ответы не могут быть пустыми")
    private List<Map<String, String>> answers;
}