package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectAgentPrompt;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectAgentPromptRepository extends JpaRepository<ProjectAgentPrompt, Long> {
    List<ProjectAgentPrompt> findAllByProject(Project project);
    Optional<ProjectAgentPrompt> findByProjectAndStepName(Project project, StepName stepName);
    void deleteAllByProject(Project project);
}
