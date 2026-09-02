package ru.sergalas.orchestrator.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.entity.TaskTemplate;
import ru.sergalas.orchestrator.service.FileStructureTemplateService;
import ru.sergalas.orchestrator.service.OrchestratorService;
import ru.sergalas.orchestrator.service.TaskTemplateService;
import ru.sergalas.orchestrator.util.Either;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: ProjectWizardController 3-Step Wizard Flow")
class ProjectWizardControllerTest {

    @Mock private TaskTemplateService taskTemplateService;
    @Mock private FileStructureTemplateService structureTemplateService;
    @Mock private OrchestratorService orchestratorService;

    @InjectMocks
    private ProjectWizardController wizardController;

    @Test
    @DisplayName("Step 1 GET renders template selection view")
    void testStep1Get() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        when(taskTemplateService.findAll()).thenReturn(Collections.emptyList());
        when(structureTemplateService.findAll()).thenReturn(Collections.emptyList());

        String view = wizardController.step1(model, session);

        assertThat(view).isEqualTo("wizard/step1-template");
        assertThat(model.getAttribute("taskTemplates")).isNotNull();
    }

    @Test
    @DisplayName("Step 1 POST saves template into session and redirects to Step 2")
    void testStep1Post() {
        MockHttpSession session = new MockHttpSession();
        TaskTemplate template = TaskTemplate.builder().id(7L).name("Template 7").content("Task Content").build();
        when(taskTemplateService.findById(7L)).thenReturn(template);

        String view = wizardController.processStep1(7L, session);

        assertThat(view).isEqualTo("redirect:/wizard/step2");
        assertThat(session.getAttribute("wizard_templateId")).isEqualTo(7L);
        assertThat(session.getAttribute("wizard_taskContent")).isEqualTo("Task Content");
    }

    @Test
    @DisplayName("Step 2 GET redirects to Step 1 if session has expired or is empty")
    void testStep2GetSessionEmptyRedirects() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        String view = wizardController.step2(model, session);

        assertThat(view).isEqualTo("redirect:/wizard/step1");
    }

    @Test
    @DisplayName("Step 3 POST launches generation pipeline and redirects to project details")
    void testStep3CreateSuccess() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("wizard_taskContent", "Final technical requirements");
        session.setAttribute("wizard_templateId", 2L);

        UserDetails userDetails = new User("admin", "password", Collections.emptyList());
        Model model = new ConcurrentModel();

        when(orchestratorService.startGeneration(any(ProjectCreateRequest.class), eq("admin")))
                .thenReturn(Either.right(500L));

        String view = wizardController.createAndRun("My Final App", "App description", List.of(0), userDetails, session, model);

        assertThat(view).isEqualTo("redirect:/projects/500");
        assertThat(session.getAttribute("wizard_taskContent")).isNull();
        verify(orchestratorService).startGeneration(any(ProjectCreateRequest.class), eq("admin"));
    }
}