package ru.sergalas.orchestrator.service;

import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.entity.Project;

import java.util.List;

public interface ProjectService {
    List<Project> findByUser(Long userId);
    Project findById(Long id);
    Project createProject(ProjectCreateRequest request, String username);
    void deleteProject(Long id);
}