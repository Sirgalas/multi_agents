package ru.sergalas.orchestrator.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ArchitectQuestionsResponse {
    private Long questionId;
    private Long projectId;
    private List<Map<String, String>> questions;
    private String status;
}