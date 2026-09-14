package ru.sergalas.orchestrator.dto.internal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeneratedFile {
    private String path;
    private String content;
    private String language;
}