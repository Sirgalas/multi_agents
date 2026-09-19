package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentPromptRepository extends JpaRepository<AgentPrompt, Long> {
    List<AgentPrompt> findAllByOrderByStepNameAscNameAsc();
    List<AgentPrompt> findAllByStepNameOrderByIsDefaultDescNameAsc(StepName stepName);
    Optional<AgentPrompt> findFirstByStepNameAndIsFinalAndIsDefaultTrue(StepName stepName, Boolean isFinal);
    Optional<AgentPrompt> findFirstByStepNameAndIsDefaultTrue(StepName stepName);
    Optional<AgentPrompt> findByNameIgnoreCase(String name);
}
