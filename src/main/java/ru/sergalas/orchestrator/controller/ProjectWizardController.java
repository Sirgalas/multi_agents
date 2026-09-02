package ru.sergalas.orchestrator.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.config.McpPresets;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.entity.TaskTemplate;
import ru.sergalas.orchestrator.service.FileStructureTemplateService;
import ru.sergalas.orchestrator.service.OrchestratorService;
import ru.sergalas.orchestrator.service.TaskTemplateService;
import ru.sergalas.orchestrator.util.Either;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/wizard")
@RequiredArgsConstructor
public class ProjectWizardController {

    private final TaskTemplateService taskTemplateService;
    private final FileStructureTemplateService structureTemplateService;
    private final OrchestratorService orchestratorService;

    // STEP 1: Select Task Template
    @GetMapping("/step1")
    public String step1(Model model, HttpSession session) {
        model.addAttribute("taskTemplates", taskTemplateService.findAll());
        model.addAttribute("structureTemplates", structureTemplateService.findAll());
        return "wizard/step1-template";
    }

    @PostMapping("/step1")
    public String processStep1(@RequestParam("templateId") Long templateId, HttpSession session) {
        TaskTemplate template = taskTemplateService.findById(templateId);
        session.setAttribute("wizard_templateId", templateId);
        session.setAttribute("wizard_taskContent", template.getContent());
        session.setAttribute("wizard_templateName", template.getName());
        return "redirect:/wizard/step2";
    }

    // STEP 2: WYSIWYG / Markdown Editor
    @GetMapping("/step2")
    public String step2(Model model, HttpSession session) {
        String content = (String) session.getAttribute("wizard_taskContent");
        Long templateId = (Long) session.getAttribute("wizard_templateId");
        if (content == null) {
            return "redirect:/wizard/step1";
        }
        model.addAttribute("content", content);
        model.addAttribute("templateId", templateId);
        return "wizard/step2-edit";
    }

    @PostMapping("/step2")
    public String processStep2(@RequestParam("content") String content, HttpSession session) {
        session.setAttribute("wizard_taskContent", content);
        return "redirect:/wizard/step3";
    }

    // STEP 3: Finalize and Launch
    @GetMapping("/step3")
    public String step3(Model model, HttpSession session) {
        String content = (String) session.getAttribute("wizard_taskContent");
        if (content == null) {
            return "redirect:/wizard/step1";
        }
        model.addAttribute("defaultMcpServers", McpPresets.DEFAULT_MCP_SERVERS);
        model.addAttribute("templateName", session.getAttribute("wizard_templateName"));
        return "wizard/step3-finalize";
    }

    @PostMapping("/step3/create")
    public String createAndRun(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "selectedMcpIndices", required = false) List<Integer> selectedMcpIndices,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpSession session,
            Model model
    ) {
        String content = (String) session.getAttribute("wizard_taskContent");
        Long templateId = (Long) session.getAttribute("wizard_templateId");

        List<McpServerRequest> mcpServers = new ArrayList<>();
        if (selectedMcpIndices != null) {
            for (Integer index : selectedMcpIndices) {
                if (index >= 0 && index < McpPresets.DEFAULT_MCP_SERVERS.size()) {
                    mcpServers.add(McpPresets.DEFAULT_MCP_SERVERS.get(index));
                }
            }
        }

        ProjectCreateRequest request = ProjectCreateRequest.builder()
                .name(name)
                .description(description)
                .taskTemplateId(templateId)
                .taskContent(content)
                .mcpServers(mcpServers)
                .build();

        Either<ArchitectQuestionsResponse, Long> result = orchestratorService.startGeneration(request, userDetails.getUsername());

        // Clear wizard session
        session.removeAttribute("wizard_taskContent");
        session.removeAttribute("wizard_templateId");
        session.removeAttribute("wizard_templateName");

        if (result.isLeft()) {
            model.addAttribute("questionsResponse", result.getLeft());
            return "project/questions";
        }

        return "redirect:/projects/" + result.getRight();
    }
}