package ru.sergalas.orchestrator.service;

import org.springframework.security.core.Authentication;
import ru.sergalas.orchestrator.dto.request.UserAnswerRequest;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.dto.response.OrchestratorStatusResponse;
import ru.sergalas.orchestrator.entity.enums.StepName;

public interface OrchestratorService {
    /**
     * Запуск полной цепочки: ARCHITECT → WORKER → TESTER → HELPER.
     * Если ARCHITECT вернул уточняющий вопрос, цепочка останавливается.
     */
    OrchestratorStatusResponse runFullPipeline(Long projectId, Authentication auth);

    /**
     * Запуск одного конкретного шага.
     */
    AgentStepResponse runStep(Long projectId, StepName step, Authentication auth);

    /**
     * Продолжение цепочки после ответа пользователя на уточняющий вопрос.
     */
    OrchestratorStatusResponse continueWithAnswer(UserAnswerRequest request, Authentication auth);

    /**
     * Получить текущий статус выполнения оркестратора.
     */
    OrchestratorStatusResponse getStatus(Long projectId, Authentication auth);
}