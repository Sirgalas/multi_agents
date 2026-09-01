package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.ProjectContext;

import java.util.List;

@Repository
public interface ProjectContextRepository extends JpaRepository<ProjectContext, Long> {
    List<ProjectContext> findAllByProjectIdOrderByCreatedAtAsc(Long projectId);
}