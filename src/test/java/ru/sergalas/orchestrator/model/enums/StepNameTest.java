package ru.sergalas.orchestrator.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Unit Tests: StepName Enum Transitions")
class StepNameTest {

    @ParameterizedTest(name = "Step {0} should transition to {1}")
    @CsvSource({
            "ARCHITECT, WORKER",
            "WORKER, TESTER",
            "TESTER, HELPER"
    })
    @DisplayName("Verify sequential state machine transitions")
    void next_ShouldTransitionToNextStepInPipeline(StepName current, StepName expectedNext) {
        assertEquals(expectedNext, current.next());
    }

    @Test
    @DisplayName("Edge Case: HELPER is the terminal step and should return null")
    void next_HelperStep_ShouldReturnNull() {
        assertNull(StepName.HELPER.next(), "Terminal step HELPER must transition to null");
    }
}