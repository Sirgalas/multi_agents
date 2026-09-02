package ru.sergalas.orchestrator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.entity.FileStructureTemplate;
import ru.sergalas.orchestrator.repository.FileStructureTemplateRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileStructureTemplateServiceImpl implements FileStructureTemplateService {

    private final FileStructureTemplateRepository repository;

    @Override
    public List<FileStructureTemplate> findAll() {
        return repository.findAll();
    }

    @Override
    public FileStructureTemplate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File structure template not found with id: " + id));
    }
}