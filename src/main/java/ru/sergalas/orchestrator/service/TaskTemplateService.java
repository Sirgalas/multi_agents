package ru.sergalas.orchestrator.service;

import ru.sergalas.orchestrator.entity.TaskTemplate;

import java.util.List;

public interface TaskTemplateService {
    List<TaskTemplate> findAll();
    TaskTemplate findById(Long id);
}