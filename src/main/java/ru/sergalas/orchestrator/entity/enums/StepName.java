package ru.sergalas.orchestrator.entity.enums;

import ru.sergalas.orchestrator.service.orchestrator.StepOrderRegistry;

public enum StepName {
    INTERVIEWER,
    ARCHITECT,
    BACKEND_ANALYST,
    FRONTEND_ANALYST,
    DESIGNER,
    BACKEND_DEVELOPER,
    FRONTEND_DEVELOPER,
    TESTER,
    HELPER,
    ARCHIVER;

    /**
     * Возвращает порядковый номер шага из централизованного реестра шагов.
     */
    public int getOrder() {
        return StepOrderRegistry.getOrder(this);
    }

    /**
     * Проверяет, является ли шаг аналитическим (проектирование/дизайн).
     */
    public boolean isAnalyst() {
        return this == BACKEND_ANALYST || this == FRONTEND_ANALYST || this == DESIGNER;
    }

    /**
     * Проверяет, является ли шаг разработкой кода.
     */
    public boolean isDeveloper() {
        return this == BACKEND_DEVELOPER || this == FRONTEND_DEVELOPER;
    }
}