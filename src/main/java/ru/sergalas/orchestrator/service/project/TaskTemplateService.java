package ru.sergalas.orchestrator.service.project;

import ru.sergalas.orchestrator.entity.TaskTemplate;

import java.util.List;

public interface TaskTemplateService {
    List<TaskTemplate> getAllTemplates();
    TaskTemplate getTemplateById(Long id);
    TaskTemplate saveTemplate(TaskTemplate template);
}