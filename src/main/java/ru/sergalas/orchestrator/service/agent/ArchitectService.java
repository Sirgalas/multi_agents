package ru.sergalas.orchestrator.service.agent;

import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.entity.Project;

import java.util.List;
import java.util.Map;

public interface ArchitectService {
    void analyzeTask(Long projectId);
    ArchitectQuestionsResponse getPendingQuestions(Long projectId);
    void processAnswers(Long questionId, List<Map<String, String>> answers);
    boolean isCompleted(Project project);
}