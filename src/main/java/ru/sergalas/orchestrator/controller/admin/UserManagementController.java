package ru.sergalas.orchestrator.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.CreateUserRequest;
import ru.sergalas.orchestrator.service.user.UserService;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("createUserRequest", new CreateUserRequest());
        return "admin/users";
    }

    @PostMapping
    public String createUser(
            @Valid @ModelAttribute("createUserRequest") CreateUserRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userService.getAllUsers());
            return "admin/users";
        }
        userService.createUser(request);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}