package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByUserOrderByCreatedAtDesc(User user);
    List<Project> findAllByOrderByCreatedAtDesc();
}