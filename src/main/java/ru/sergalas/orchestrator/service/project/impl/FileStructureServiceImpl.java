package ru.sergalas.orchestrator.service.project.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.entity.FileStructureTemplate;
import ru.sergalas.orchestrator.exception.TemplateNotFoundException;
import ru.sergalas.orchestrator.repository.FileStructureTemplateRepository;
import ru.sergalas.orchestrator.service.project.FileStructureService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileStructureServiceImpl implements FileStructureService {

    private final FileStructureTemplateRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<FileStructureTemplate> getAllTemplates() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public FileStructureTemplate getTemplateById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("File structure template not found: " + id));
    }

    @Override
    @Transactional
    public FileStructureTemplate saveTemplate(FileStructureTemplate template) {
        return repository.save(template);
    }
}