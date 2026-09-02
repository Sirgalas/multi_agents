package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.UserCreateRequest;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.service.UserService;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("userForm", new UserCreateRequest());
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    @PostMapping
    public String createUser(@Valid @ModelAttribute("userForm") UserCreateRequest form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }
        try {
            userService.createUser(form);
        } catch (IllegalArgumentException e) {
            result.rejectValue("username", "error.user", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}