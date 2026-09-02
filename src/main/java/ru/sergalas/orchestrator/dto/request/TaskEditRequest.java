package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEditRequest {

    private Long templateId;

    @NotBlank(message = "Task content is required")
    private String content;
}