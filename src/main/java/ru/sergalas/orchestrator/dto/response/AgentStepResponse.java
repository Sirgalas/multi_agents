package ru.sergalas.orchestrator.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AgentStepResponse {
    private Long id;
    private String stepName;
    private String status;
    private String prompt;
    private String response;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}