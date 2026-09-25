package ru.sergalas.orchestrator.service.agent;

import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;

import java.util.List;
import java.util.Map;

/**
 * Интерфейс взаимодействия с агентом-архитектором по вопросам HITL (Human-in-the-Loop).
 */
public interface ArchitectService {

    /**
     * Возвращает список ожидающих ответа вопросов архитектора для указанного проекта.
     */
    ArchitectQuestionsResponse getPendingQuestions(Long projectId);

    /**
     * Обрабатывает ответы пользователя на уточняющие вопросы и генерирует архитектурную спецификацию.
     */
    void processAnswers(Long questionId, List<Map<String, String>> answers);
}