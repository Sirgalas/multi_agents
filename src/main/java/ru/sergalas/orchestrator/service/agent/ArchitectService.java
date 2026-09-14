package ru.sergalas.orchestrator.service.agent;

import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;

import java.util.List;
import java.util.Map;

public interface ArchitectService {
    void analyzeTask(Long projectId);
    ArchitectQuestionsResponse getPendingQuestions(Long projectId);
    void processAnswers(Long questionId, List<Map<String, String>> answers);
}