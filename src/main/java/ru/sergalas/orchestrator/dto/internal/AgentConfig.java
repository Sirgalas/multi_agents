package ru.sergalas.orchestrator.dto.internal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentConfig {
    private String url;
    private String token;
    private String model;
}