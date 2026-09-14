package ru.sergalas.orchestrator.service.project;

import ru.sergalas.orchestrator.entity.FileStructureTemplate;

import java.util.List;

public interface FileStructureService {
    List<FileStructureTemplate> getAllTemplates();
    FileStructureTemplate getTemplateById(Long id);
    FileStructureTemplate saveTemplate(FileStructureTemplate template);
}