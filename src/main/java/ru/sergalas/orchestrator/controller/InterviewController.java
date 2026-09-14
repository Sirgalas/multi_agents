package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.InterviewMessageRequest;
import ru.sergalas.orchestrator.dto.response.InterviewResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.agent.InterviewerService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;

@Controller
@RequestMapping("/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewerService interviewerService;
    private final ProjectService projectService;
    private final AgentStepRepository agentStepRepository;

    @GetMapping("/{projectId}")
    public String openInterviewPage(@PathVariable Long projectId, Model model) {
        Project project = projectService.getProjectById(projectId);
        List<AgentStep> steps = agentStepRepository.findAllByProjectOrderByCreatedAtAsc(project);

        model.addAttribute("project", project);
        model.addAttribute("steps", steps);
        return "interview/chat";
    }

    @PostMapping("/api/start/{projectId}")
    @ResponseBody
    public ResponseEntity<InterviewResponse> startInterview(@PathVariable Long projectId) {
        InterviewResponse response = interviewerService.startInterview(projectId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/message")
    @ResponseBody
    public ResponseEntity<InterviewResponse> sendMessage(@Valid @RequestBody InterviewMessageRequest request) {
        InterviewResponse response = interviewerService.processAnswer(request.getProjectId(), request.getMessage());
        return ResponseEntity.ok(response);
    }
}