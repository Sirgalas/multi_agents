package ru.sergalas.orchestrator.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.CreateAgentPromptRequest;
import ru.sergalas.orchestrator.dto.request.UpdateAgentPromptRequest;
import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.service.mcp.McpServerService;
import ru.sergalas.orchestrator.service.prompt.AgentPromptService;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/prompts")
@RequiredArgsConstructor
public class AdminPromptController {

    private final AgentPromptService agentPromptService;
    private final McpServerService mcpServerService;

    @GetMapping
    public String listPrompts(
            @RequestParam(required = false) StepName step,
            Model model
    ) {
        List<AgentPrompt> prompts = (step != null)
                ? agentPromptService.getPromptsByStep(step)
                : agentPromptService.getAllPrompts();

        model.addAttribute("prompts", prompts);
        model.addAttribute("selectedStep", step);
        model.addAttribute("steps", StepName.values());
        model.addAttribute("allMcpServers", mcpServerService.getAllServers());
        if (!model.containsAttribute("createPromptRequest")) {
            model.addAttribute("createPromptRequest", new CreateAgentPromptRequest());
        }
        return "admin/prompts";
    }

    @PostMapping
    public String createPrompt(
            @Valid @ModelAttribute("createPromptRequest") CreateAgentPromptRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return listPrompts(null, model);
        }

        try {
            agentPromptService.createPrompt(request);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            return listPrompts(null, model);
        }

        return "redirect:/admin/prompts";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        AgentPrompt prompt = agentPromptService.getPromptById(id);
        UpdateAgentPromptRequest request = UpdateAgentPromptRequest.builder()
                .name(prompt.getName())
                .stepName(prompt.getStepName())
                .prompt(prompt.getPrompt())
                .isFinal(prompt.getIsFinal())
                .isDefault(prompt.getIsDefault())
                .description(prompt.getDescription())
                .mcpServerIds(prompt.getMcpServers().stream().map(McpServer::getId).toList())
                .build();

        model.addAttribute("prompt", prompt);
        model.addAttribute("updatePromptRequest", request);
        model.addAttribute("steps", StepName.values());
        model.addAttribute("allMcpServers", mcpServerService.getAllServers());
        return "admin/prompt-edit";
    }

    @PostMapping("/{id}")
    public String updatePrompt(
            @PathVariable Long id,
            @Valid @ModelAttribute("updatePromptRequest") UpdateAgentPromptRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            AgentPrompt prompt = agentPromptService.getPromptById(id);
            model.addAttribute("prompt", prompt);
            model.addAttribute("steps", StepName.values());
            model.addAttribute("allMcpServers", mcpServerService.getAllServers());
            return "admin/prompt-edit";
        }

        try {
            agentPromptService.updatePrompt(id, request);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            AgentPrompt prompt = agentPromptService.getPromptById(id);
            model.addAttribute("prompt", prompt);
            model.addAttribute("steps", StepName.values());
            model.addAttribute("allMcpServers", mcpServerService.getAllServers());
            return "admin/prompt-edit";
        }

        return "redirect:/admin/prompts";
    }

    @PostMapping("/{id}/delete")
    public String deletePrompt(@PathVariable Long id) {
        agentPromptService.deletePrompt(id);
        return "redirect:/admin/prompts";
    }
}
