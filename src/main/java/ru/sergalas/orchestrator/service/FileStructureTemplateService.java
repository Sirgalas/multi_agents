package ru.sergalas.orchestrator.service;

import ru.sergalas.orchestrator.entity.FileStructureTemplate;

import java.util.List;

public interface FileStructureTemplateService {
    List<FileStructureTemplate> findAll();
    FileStructureTemplate findById(Long id);
}