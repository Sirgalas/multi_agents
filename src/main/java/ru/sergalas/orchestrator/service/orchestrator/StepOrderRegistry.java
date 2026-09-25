package ru.sergalas.orchestrator.service.orchestrator;

import org.springframework.stereotype.Component;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.service.agent.AgentsService;

import java.util.*;

/**
 * Централизованный реестр последовательности шагов мультиагентного конвейера.
 * Единый источник правды (Single Source of Truth) для порядка выполнения агентов,
 * их фаз и определения шагов при откате/перезапуске вместо разрозненных аннотаций @Order.
 */
@Component
public class StepOrderRegistry {

    /**
     * Центральная неизменяемая карта последовательности шагов в конвейере.
     */
    private static final Map<StepName, Integer> STEP_ORDER_MAP;

    static {
        Map<StepName, Integer> map = new LinkedHashMap<>();
        map.put(StepName.INTERVIEWER, 0);
        map.put(StepName.ARCHITECT, 1);
        map.put(StepName.BACKEND_ANALYST, 2);
        map.put(StepName.FRONTEND_ANALYST, 3);
        map.put(StepName.DESIGNER, 4);
        map.put(StepName.BACKEND_DEVELOPER, 5);
        map.put(StepName.FRONTEND_DEVELOPER, 6);
        map.put(StepName.TESTER, 7);
        map.put(StepName.HELPER, 8);
        map.put(StepName.ARCHIVER, 9);
        STEP_ORDER_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Возвращает порядковый номер шага в конвейере.
     */
    public static int getOrder(StepName stepName) {
        if (stepName == null) {
            return Integer.MAX_VALUE;
        }
        return STEP_ORDER_MAP.getOrDefault(stepName, Integer.MAX_VALUE);
    }

    /**
     * Возвращает центральную карту порядка всех шагов.
     */
    public Map<StepName, Integer> getStepOrderMap() {
        return STEP_ORDER_MAP;
    }

    /**
     * Компаратор для сортировки агентов по их шагу.
     */
    public Comparator<AgentsService> agentComparator() {
        return Comparator.comparingInt(agent -> getOrder(agent != null ? agent.getStepName() : null));
    }

    /**
     * Сортирует коллекцию агентов в соответствии с централизованным порядком шагов.
     */
    public List<AgentsService> sortAgents(Collection<AgentsService> agents) {
        if (agents == null) {
            return List.of();
        }
        return agents.stream()
                .sorted(agentComparator())
                .toList();
    }

    /**
     * Возвращает список шагов исполнения конвейера (начиная с указанного шага),
     * подлежащих сбросу при откате.
     */
    public static List<StepName> getDownstreamSteps(StepName startStep) {
        if (startStep == null) {
            return List.of();
        }
        int startOrder = getOrder(startStep);
        return STEP_ORDER_MAP.entrySet().stream()
                .filter(e -> e.getKey() != StepName.INTERVIEWER && e.getKey() != StepName.ARCHIVER)
                .filter(e -> e.getValue() >= startOrder)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Возвращает список шагов исполнения конвейера по строковому названию шага.
     */
    public static List<StepName> getDownstreamSteps(String stepNameStr) {
        if (stepNameStr == null || stepNameStr.isBlank()) {
            return getDownstreamSteps(StepName.ARCHITECT);
        }
        try {
            StepName step = StepName.valueOf(stepNameStr.trim().toUpperCase());
            return getDownstreamSteps(step);
        } catch (IllegalArgumentException e) {
            return getDownstreamSteps(StepName.ARCHITECT);
        }
    }
}
