package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentStepRepository extends JpaRepository<AgentStep, Long> {
    List<AgentStep> findByProjectIdOrderByCreatedAtAsc(Long projectId);
    Optional<AgentStep> findFirstByProjectIdAndStepNameOrderByCreatedAtDesc(Long projectId, StepName stepName);
}