package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.sergalas.orchestrator.dto.request.UserAnswerRequest;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.dto.response.OrchestratorStatusResponse;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.service.OrchestratorService;

@RestController
@RequestMapping("/projects/{id}")
@RequiredArgsConstructor
public class OrchestratorController {

    private final OrchestratorService orchestratorService;

    @PostMapping("/run")
    public ResponseEntity<OrchestratorStatusResponse> runPipeline(@PathVariable("id") Long projectId, Authentication auth) {
        OrchestratorStatusResponse response = orchestratorService.runFullPipeline(projectId, auth);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/run/{step}")
    public ResponseEntity<AgentStepResponse> runStep(
            @PathVariable("id") Long projectId,
            @PathVariable("step") StepName step,
            Authentication auth
    ) {
        AgentStepResponse response = orchestratorService.runStep(projectId, step, auth);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/answer")
    public ResponseEntity<OrchestratorStatusResponse> submitAnswer(
            @PathVariable("id") Long projectId,
            @Valid @RequestBody UserAnswerRequest request,
            Authentication auth
    ) {
        if (!projectId.equals(request.projectId())) {
            throw new IllegalArgumentException("Project ID in path does not match request body");
        }
        OrchestratorStatusResponse response = orchestratorService.continueWithAnswer(request, auth);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<OrchestratorStatusResponse> status(@PathVariable("id") Long projectId, Authentication auth) {
        OrchestratorStatusResponse response = orchestratorService.getStatus(projectId, auth);
        return ResponseEntity.ok(response);
    }
}