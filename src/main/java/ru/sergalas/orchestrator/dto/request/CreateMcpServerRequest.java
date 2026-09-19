package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sergalas.orchestrator.entity.enums.McpTarget;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMcpServerRequest {

    @NotBlank(message = "Название сервера обязательно")
    private String name;

    @NotBlank(message = "URL сервера обязателен")
    private String url;

    @NotNull(message = "Сфера применения (target) обязательна")
    @Builder.Default
    private McpTarget target = McpTarget.COMMON;

    private String token;

    private String description;
}
