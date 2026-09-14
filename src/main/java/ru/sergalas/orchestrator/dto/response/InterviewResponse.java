package ru.sergalas.orchestrator.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterviewResponse {
    private Long stepId;
    private String question;
    private Boolean isFinalized;
    private String generatedTaskMarkdown;
}