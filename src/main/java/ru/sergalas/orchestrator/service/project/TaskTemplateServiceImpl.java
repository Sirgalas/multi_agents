package ru.sergalas.orchestrator.service.project;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.entity.TaskTemplate;
import ru.sergalas.orchestrator.exception.TemplateNotFoundException;
import ru.sergalas.orchestrator.repository.TaskTemplateRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TaskTemplateRepository taskTemplateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TaskTemplate> getAllTemplates() {
        return taskTemplateRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskTemplate getTemplateById(Long id) {
        return taskTemplateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Task template not found: " + id));
    }

    @Override
    @Transactional
    public TaskTemplate saveTemplate(TaskTemplate template) {
        return taskTemplateRepository.save(template);
    }
}