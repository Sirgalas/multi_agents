package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.StepName;

import ru.sergalas.orchestrator.entity.enums.StepStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentStepRepository extends JpaRepository<AgentStep, Long> {
    List<AgentStep> findAllByProjectOrderByCreatedAtAsc(Project project);
    Optional<AgentStep> findFirstByProjectAndStepNameOrderByCreatedAtDesc(Project project, StepName stepName);
    boolean existsByProjectAndStepNameAndStatus(Project project, StepName stepName, StepStatus status);
    void deleteAllByProject(Project project);
    void deleteAllByProjectAndStepNameIn(Project project, java.util.Collection<StepName> stepNames);
}