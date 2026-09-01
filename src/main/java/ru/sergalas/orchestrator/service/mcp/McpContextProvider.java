package ru.sergalas.orchestrator.service.mcp;

import ru.sergalas.orchestrator.entity.enums.TransportType;

public interface McpContextProvider {
    /**
     * Собирает документацию и правила из всех активных MCP-серверов проекта.
     * @param projectId ID проекта
     * @return Форматированный текстовый блок для инжекции в промпт
     */
    String collectContext(Long projectId);

    /**
     * Проверяет доступность конкретного MCP-сервера.
     * @param serverUrl URL сервера
     * @param transportType Тип транспорта
     * @return true, если сервер доступен
     */
    boolean isReachable(String serverUrl, TransportType transportType);
}