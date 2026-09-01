package ru.sergalas.orchestrator.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiConfig: Маппинг AI-моделей по ролям агентов")
class AiConfigTest {

    @Test
    @DisplayName("Возврат моделей по умолчанию при пустой конфигурации")
    void getModelForStep_DefaultMappingsWhenEmpty() {
        AiConfig config = new AiConfig();

        assertThat(config.getModelForStep(StepName.ARCHITECT)).isEqualTo("cc/claude-sonnet-4-6");
        assertThat(config.getModelForStep(StepName.WORKER)).isEqualTo("ag/gemini-3.7-flash-high");
        assertThat(config.getModelForStep(StepName.TESTER)).isEqualTo("ag/gemini-3.7-flash-high");
        assertThat(config.getModelForStep(StepName.HELPER)).isEqualTo("ag/gemini-3.6-flash-high");
    }

    @Test
    @DisplayName("Возврат переопределенных моделей из конфигурации")
    void getModelForStep_CustomMappings() {
        AiConfig config = new AiConfig();
        config.setModels(Map.of(
                "architect", "custom/architect-model",
                "worker", "custom/worker-model"
        ));

        assertThat(config.getModelForStep(StepName.ARCHITECT)).isEqualTo("custom/architect-model");
        assertThat(config.getModelForStep(StepName.WORKER)).isEqualTo("custom/worker-model");
        // Fallback на дефолтную модель при отсутствии ключа
        assertThat(config.getModelForStep(StepName.TESTER)).isEqualTo("cc/claude-sonnet-4-6");
    }
}