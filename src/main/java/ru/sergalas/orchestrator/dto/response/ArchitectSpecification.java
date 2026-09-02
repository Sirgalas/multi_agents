package ru.sergalas.orchestrator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchitectSpecification {
    private String structureTree;
    private String architectureSpec;
    private String dtoList;
    private String serviceContracts;
}