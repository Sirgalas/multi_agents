package ru.sergalas.orchestrator.orchestrator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.sergalas.orchestrator.enums.StepName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentRole Configuration & Mapping Tests")
class AgentRoleTest {

    @ParameterizedTest
    @EnumSource(StepName.class)
    @DisplayName("Should map every StepName to valid AgentRole with non-null configurations")
    void shouldMapAllStepNames(StepName stepName) {
        AgentRole role = AgentRole.fromStepName(stepName);

        assertThat(role).isNotNull();
        assertThat(role.getStepName()).isEqualTo(stepName);
        assertThat(role.getModelId()).isNotBlank();
        assertThat(role.getTemperature()).isBetween(0.0, 1.0);
        assertThat(role.getMaxTokens()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Verify precise model assignments per specification")
    void verifyModelAssignments() {
        assertThat(AgentRole.ARCHITECT.getModelId()).isEqualTo("cc/claude-sonnet-4-6");
        assertThat(AgentRole.WORKER.getModelId()).isEqualTo("ag/gemini-3.7-flash-high");
        assertThat(AgentRole.TESTER.getModelId()).isEqualTo("ag/gemini-3.7-flash-high");
        assertThat(AgentRole.HELPER.getModelId()).isEqualTo("ag/gemini-3.6-flash-high");
    }
}