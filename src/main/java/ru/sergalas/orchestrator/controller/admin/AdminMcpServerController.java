package ru.sergalas.orchestrator.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.CreateMcpServerRequest;
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.service.mcp.McpServerService;

@Slf4j
@Controller
@RequestMapping("/admin/mcp-servers")
@RequiredArgsConstructor
public class AdminMcpServerController {

    private final McpServerService mcpServerService;

    @GetMapping
    public String listMcpServers(Model model) {
        model.addAttribute("servers", mcpServerService.getAllServers());
        model.addAttribute("createServerRequest", new CreateMcpServerRequest());
        model.addAttribute("targets", McpTarget.values());
        return "admin/mcp-servers";
    }

    @PostMapping
    public String createMcpServer(
            @Valid @ModelAttribute("createServerRequest") CreateMcpServerRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("servers", mcpServerService.getAllServers());
            model.addAttribute("targets", McpTarget.values());
            return "admin/mcp-servers";
        }

        try {
            mcpServerService.createServer(request);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            model.addAttribute("servers", mcpServerService.getAllServers());
            model.addAttribute("targets", McpTarget.values());
            return "admin/mcp-servers";
        }

        return "redirect:/admin/mcp-servers";
    }

    @PostMapping("/{id}/delete")
    public String deleteMcpServer(@PathVariable Long id) {
        mcpServerService.deleteServer(id);
        return "redirect:/admin/mcp-servers";
    }
}
