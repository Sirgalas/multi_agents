package ru.sergalas.orchestrator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.service.project.ProjectService;
import ru.sergalas.orchestrator.service.user.UserService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ProjectService projectService;
    private final UserService userService;

    @GetMapping("/")
    public String index(Model model) {
        User currentUser = userService.getCurrentUser();
        List<Project> projects = projectService.getProjectsForUser(currentUser);
        model.addAttribute("projects", projects);
        model.addAttribute("user", currentUser);
        return "dashboard/index";
    }
}