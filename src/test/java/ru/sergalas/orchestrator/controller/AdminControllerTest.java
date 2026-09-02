package ru.sergalas.orchestrator.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import ru.sergalas.orchestrator.dto.request.UserCreateRequest;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.service.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: AdminController")
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private AdminController adminController;

    @Test
    @DisplayName("listUsers adds all users to model and returns admin/users view")
    void testListUsers() {
        when(userService.findAll()).thenReturn(List.of(User.builder().id(1L).username("admin").build()));
        Model model = new ConcurrentModel();

        String view = adminController.listUsers(model);

        assertThat(view).isEqualTo("admin/users");
        assertThat(model.getAttribute("users")).isNotNull();
    }

    @Test
    @DisplayName("createUser redirects to /admin/users on valid submission")
    void testCreateUserSuccess() {
        UserCreateRequest form = UserCreateRequest.builder().username("john").password("p123").role(Role.ROLE_USER).build();
        Model model = new ConcurrentModel();

        when(bindingResult.hasErrors()).thenReturn(false);

        String view = adminController.createUser(form, bindingResult, model);

        assertThat(view).isEqualTo("redirect:/admin/users");
        verify(userService).createUser(form);
    }

    @Test
    @DisplayName("createUser returns user-form view on validation errors")
    void testCreateUserValidationErrors() {
        UserCreateRequest form = UserCreateRequest.builder().build();
        Model model = new ConcurrentModel();

        when(bindingResult.hasErrors()).thenReturn(true);

        String view = adminController.createUser(form, bindingResult, model);

        assertThat(view).isEqualTo("admin/user-form");
        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("deleteUser delegates to service and redirects")
    void testDeleteUser() {
        String view = adminController.deleteUser(15L);

        assertThat(view).isEqualTo("redirect:/admin/users");
        verify(userService).deleteUser(15L);
    }
}