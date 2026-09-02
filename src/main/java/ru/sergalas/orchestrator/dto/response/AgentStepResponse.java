package ru.sergalas.orchestrator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStepResponse {
    private Long id;
    private StepName stepName;
    private StepStatus status;
    private String prompt;
    private String response;
    private LocalDateTime createdAt;
}