package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgentPromptRequest {

    @NotBlank(message = "Название промпта обязательно")
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    private String name;

    @NotNull(message = "Шаг агента обязателен")
    private StepName stepName;

    @NotBlank(message = "Текст промпта обязателен")
    private String prompt;

    @Builder.Default
    private Boolean isFinal = false;

    @Builder.Default
    private Boolean isDefault = false;

    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    private String description;

    @Builder.Default
    private List<Long> mcpServerIds = new ArrayList<>();
}
