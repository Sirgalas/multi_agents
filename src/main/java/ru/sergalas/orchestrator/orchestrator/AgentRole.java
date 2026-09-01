package ru.sergalas.orchestrator.orchestrator;

import lombok.Getter;
import ru.sergalas.orchestrator.enums.StepName;

@Getter
public enum AgentRole {
    ARCHITECT(
        StepName.ARCHITECT,
        "cc/claude-sonnet-4-6",
        0.3,
        8192
    ),
    WORKER(
        StepName.WORKER,
        "ag/gemini-3.7-flash-high",
        0.2,
        16384
    ),
    TESTER(
        StepName.TESTER,
        "ag/gemini-3.7-flash-high",
        0.1,
        8192
    ),
    HELPER(
        StepName.HELPER,
        "ag/gemini-3.6-flash-high",
        0.2,
        8192
    );

    private final StepName stepName;
    private final String modelId;
    private final double temperature;
    private final int maxTokens;

    AgentRole(StepName stepName, String modelId, double temperature, int maxTokens) {
        this.stepName = stepName;
        this.modelId = modelId;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public static AgentRole fromStepName(StepName stepName) {
        for (AgentRole role : values()) {
            if (role.getStepName() == stepName) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown step name: " + stepName);
    }
}