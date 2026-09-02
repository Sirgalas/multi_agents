package ru.sergalas.orchestrator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.entity.TaskTemplate;
import ru.sergalas.orchestrator.repository.TaskTemplateRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TaskTemplateRepository repository;

    @Override
    public List<TaskTemplate> findAll() {
        return repository.findAll();
    }

    @Override
    public TaskTemplate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task template not found with id: " + id));
    }
}