package ru.sergalas.orchestrator.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;
import ru.sergalas.orchestrator.entity.FileStructureTemplate;
import ru.sergalas.orchestrator.service.McpService;
import ru.sergalas.orchestrator.util.Either;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: ArchitectAgentImpl (Claude Sonnet 4.6)")
class ArchitectAgentImplTest {

    @Mock
    private ChatModel architectChatModel;

    @Mock
    private McpService mcpService;

    private ObjectMapper objectMapper;
    private ArchitectAgentImpl architectAgent;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        architectAgent = new ArchitectAgentImpl(architectChatModel, mcpService, objectMapper);
    }

    @Test
    @DisplayName("analyze returns Either.left with clarifying questions when LLM outputs JSON questions")
    void testAnalyzeReturnsClarifyingQuestions() {
        String jsonResponse = """
                ```json
                {
                  "questions": [
                    {
                      "id": "q1",
                      "text": "Do you require JWT or Session auth?",
                      "type": "CHOICE",
                      "options": ["JWT", "SESSION"]
                    }
                  ]
                }
                ```
                """;

        when(architectChatModel.call(anyString())).thenReturn(jsonResponse);

        FileStructureTemplate structure = FileStructureTemplate.builder().structureTree("├── src/").build();

        Either<ArchitectQuestionsResponse, ArchitectSpecification> result =
                architectAgent.analyze(1L, "Build multi-tenant SaaS", structure, Collections.emptyList(), Collections.emptyList());

        assertThat(result.isLeft()).isTrue();
        ArchitectQuestionsResponse questions = result.getLeft();
        assertThat(questions.getProjectId()).isEqualTo(1L);
        assertThat(questions.getQuestions()).hasSize(1);
        assertThat(questions.getQuestions().get(0).getId()).isEqualTo("q1");
    }

    @Test
    @DisplayName("analyze returns Either.right with ArchitectSpecification when task is clear")
    void testAnalyzeReturnsFullSpecification() {
        String specResponse = """
                # Architectural Specification
                [FILE: src/main/java/ru/sergalas/Demo.java]
                ```java
                package ru.sergalas;
                ```
                """;

        when(architectChatModel.call(anyString())).thenReturn(specResponse);

        Either<ArchitectQuestionsResponse, ArchitectSpecification> result =
                architectAgent.analyze(1L, "Standard Spring Task", null, Collections.emptyList(), Collections.emptyList());

        assertThat(result.isRight()).isTrue();
        ArchitectSpecification spec = result.getRight();
        assertThat(spec.getArchitectureSpec()).isEqualTo(specResponse);
    }

    @Test
    @DisplayName("finalizeSpec calls chat model and packages finalized specification")
    void testFinalizeSpec() {
        when(architectChatModel.call(anyString())).thenReturn("Finalized Spec Content");

        ArchitectSpecification spec = architectAgent.finalizeSpec(
                "Task",
                Map.of("q1", "JWT"),
                FileStructureTemplate.builder().structureTree("tree").build(),
                Collections.emptyList()
        );

        assertThat(spec).isNotNull();
        assertThat(spec.getArchitectureSpec()).isEqualTo("Finalized Spec Content");
        assertThat(spec.getStructureTree()).isEqualTo("tree");
    }
}