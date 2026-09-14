package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.ArchitectQuestion;
import ru.sergalas.orchestrator.entity.Project;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArchitectQuestionRepository extends JpaRepository<ArchitectQuestion, Long> {
    List<ArchitectQuestion> findAllByProjectOrderByCreatedAtDesc(Project project);
    Optional<ArchitectQuestion> findFirstByProjectAndStatus(Project project, String status);
}