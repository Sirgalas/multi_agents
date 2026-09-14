package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.ArchitectAnswerRequest;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.service.agent.ArchitectService;
import ru.sergalas.orchestrator.service.orchestrator.PipelineExecutor;

import java.util.Map;

@Controller
@RequestMapping("/api/projects/{projectId}/architect")
@RequiredArgsConstructor
public class ArchitectQuestionController {

    private final ArchitectService architectService;
    private final PipelineExecutor pipelineExecutor;

    @GetMapping("/questions")
    @ResponseBody
    public ResponseEntity<ArchitectQuestionsResponse> getPendingQuestions(@PathVariable Long projectId) {
        ArchitectQuestionsResponse response = architectService.getPendingQuestions(projectId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/answers")
    @ResponseBody
    public ResponseEntity<Map<String, String>> submitAnswers(
            @PathVariable Long projectId,
            @Valid @RequestBody ArchitectAnswerRequest request
    ) {
        architectService.processAnswers(request.getQuestionId(), request.getAnswers());
        // Resume pipeline execution
        pipelineExecutor.runPipeline(projectId);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Answers recorded, pipeline resumed"));
    }
}