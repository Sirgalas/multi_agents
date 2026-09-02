package ru.sergalas.orchestrator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationResultResponse {
    private Long projectId;
    private boolean success;
    private String message;
    @Builder.Default
    private List<GeneratedFile> files = new ArrayList<>();
    private String zipArchivePath;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedFile {
        private String path;
        private int linesOfCode;
    }
}