package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.FileStructureTemplate;

import java.util.Optional;

@Repository
public interface FileStructureTemplateRepository extends JpaRepository<FileStructureTemplate, Long> {
    Optional<FileStructureTemplate> findByName(String name);
}