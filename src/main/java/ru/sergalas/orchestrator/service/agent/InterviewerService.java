package ru.sergalas.orchestrator.service.agent;

import ru.sergalas.orchestrator.dto.response.InterviewResponse;

public interface InterviewerService {
    InterviewResponse startInterview(Long projectId);
    InterviewResponse processAnswer(Long projectId, String userMessage);
    String finalizeInterview(Long projectId);
}