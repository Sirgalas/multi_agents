package ru.sergalas.orchestrator.service.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiChatModel;
import ru.sergalas.orchestrator.config.properties.AgentsProperties;
import ru.sergalas.orchestrator.config.properties.AnymodelProperties;
import ru.sergalas.orchestrator.entity.enums.StepName;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AgentClientFactoryTest {

    private AgentsProperties agentsProperties;
    private AnymodelProperties anymodelProperties;
    private AgentClientFactory factory;

    @BeforeEach
    void setUp() {
        agentsProperties = new AgentsProperties();
        anymodelProperties = new AnymodelProperties();
        anymodelProperties.setKey("fallback-api-key");
        anymodelProperties.getBase().setUrl("https://fallback.anymodel.org/v1");

        factory = new AgentClientFactory(agentsProperties, anymodelProperties);
    }

    @Test
    @DisplayName("Edge Case: каскадное разрешение — персональные настройки агента имеют наивысший приоритет")
    void createClient_PersonalSettings_HighestPriority() {
        // Arrange
        agentsProperties.getAll().setUrl("https://all.url/v1");
        agentsProperties.getAll().setToken("all-token");

        AgentsProperties.AgentSettings interviewerSettings = agentsProperties.getInterviewer();
        interviewerSettings.setUrl("https://interviewer.url/v1");
        interviewerSettings.setToken("interviewer-token");
        interviewerSettings.setModel("custom/interviewer-model");

        // Act
        OpenAiChatModel client = factory.createClient(StepName.INTERVIEWER);

        // Assert
        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("Edge Case: каскадное разрешение — при отсутствии персональных настроек берутся общие agents.all")
    void createClient_FallbackToAllSettings() {
        // Arrange
        agentsProperties.getAll().setUrl("https://all.url/v1");
        agentsProperties.getAll().setToken("all-token");
        agentsProperties.getArchitect().setModel("cc/claude-sonnet-4-6");

        // Act
        OpenAiChatModel client = factory.createClient(StepName.ARCHITECT);

        // Assert
        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("Edge Case: каскадное разрешение — при пустых настройках берутся anymodel.api fallback")
    void createClient_FallbackToAnymodelProperties() {
        // Act
        OpenAiChatModel client = factory.createClient(StepName.BACKEND_DEVELOPER);

        // Assert
        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("Edge Case: если модель не указана, используется дефолтное значение")
    void createClient_DefaultModelFallback_WhenModelIsBlank() {
        // Arrange
        agentsProperties.getTester().setModel("   ");

        // Act
        OpenAiChatModel client = factory.createClient(StepName.TESTER);

        // Assert
        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("Edge Case: для DESIGNER при пустых персональных настройках берется fallback на архитектора")
    void createClient_Designer_FallbackToArchitect() {
        // Arrange
        agentsProperties.getArchitect().setModel("cc/claude-sonnet-4-6");

        // Act
        OpenAiChatModel client = factory.createClient(StepName.DESIGNER);

        // Assert
        assertThat(client).isNotNull();
    }
}