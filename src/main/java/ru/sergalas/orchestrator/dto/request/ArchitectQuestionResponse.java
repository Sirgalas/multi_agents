package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchitectQuestionResponse {

    @NotNull
    private Long projectId;

    @Builder.Default
    private Map<String, String> answers = new HashMap<>();
}