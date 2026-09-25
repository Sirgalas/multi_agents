package ru.sergalas.orchestrator.service.agent;

import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.util.Optional;

/**
 * Базовый интерфейс стратегии выполнения шагов агентами системы.
 */
public interface AgentsService {

    /**
     * Имя шага агента (например, BACKEND_ANALYST, FRONTEND_ANALYST и др.).
     */
    StepName getStepName();

    /**
     * Проверяет, требуется ли данный агент для указанного проекта (с учетом типа проекта: Fullstack, Backend-only, Frontend-only).
     *
     * @param project проект
     * @return Optional с текущим агентом, если он требуется, иначе Optional.empty()
     */
    Optional<AgentsService> isNeedAgents(Project project);

    /**
     * Проверяет, соответствует ли данный агент указанному имени шага.
     *
     * @param stepName имя шага (например, BACKEND_ANALYST)
     * @return Optional с текущим агентом, если шаг совпадает, иначе Optional.empty()
     */
    default Optional<AgentsService> isNeedAgents(String stepName) {
        if (stepName != null && getStepName() != null && getStepName().name().equalsIgnoreCase(stepName.trim())) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    /**
     * Проверяет, требуется ли данный агент для проекта и соответствует ли он указанному шагу конвейера.
     *
     * @param project проект
     * @param stepName имя шага (например, BACKEND_ANALYST)
     * @return Optional с текущим агентом, если агент подходит под шаг и проект, иначе Optional.empty()
     */
    default Optional<AgentsService> isNeedAgents(Project project, String stepName) {
        if (stepName != null && !stepName.isBlank() && isNeedAgents(stepName).isEmpty()) {
            return Optional.empty();
        }
        return isNeedAgents(project);
    }

    /**
     * Проверяет, завершен ли шаг данного агента.
     *
     * @param project проект
     * @return true, если шаг завершен
     */
    boolean isCompleted(Project project);

    /**
     * Выполняет основную работу агента (анализ, генерация кода, тесты и т.д.).
     *
     * @param projectId идентификатор проекта
     */
    void work(Long projectId);

    /**
     * Проверяет, является ли данный агент аналитиком.
     */
    default boolean isAnalyst() {
        StepName step = getStepName();
        return step != null && step.isAnalyst();
    }

    /**
     * Проверяет, является ли данный агент разработчиком.
     */
    default boolean isDeveloper() {
        StepName step = getStepName();
        return step != null && step.isDeveloper();
    }
}
