package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.TaskTemplate;

@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {
}