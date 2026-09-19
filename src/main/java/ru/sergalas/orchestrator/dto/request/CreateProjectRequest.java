package ru.sergalas.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class CreateProjectRequest {
    
    @NotBlank(message = "Название проекта обязательно")
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    private String name;
    
    @Size(max = 5000, message = "Описание не должно превышать 5000 символов")
    private String description;
    
    private Long taskTemplateId;
    
    private Long fileStructureTemplateId;
    
    @NotBlank(message = "Содержимое ТЗ обязательно")
    private String taskContent;
    
    private List<Long> mcpServerIds = new ArrayList<>();
    
    private List<String> defaultMcpServerNames = new ArrayList<>();

    private Map<StepName, Long> promptIds = new HashMap<>();
}