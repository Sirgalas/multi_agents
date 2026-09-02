package ru.sergalas.orchestrator.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SpringAiConfig {

    @Value("${anymodel.api.key}")
    private String apiKey;

    @Value("${anymodel.base.url}")
    private String baseUrl;

    private OpenAiApi createOpenAiApi() {
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .restClientBuilder(RestClient.builder())
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("architectChatModel")
    @Primary
    public ChatModel architectChatModel() {
        return OpenAiChatModel.builder()
                .openAiApi(createOpenAiApi())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("cc/claude-sonnet-4-6")
                        .temperature(0.3)
                        .maxTokens(8000)
                        .build())
                .build();
    }

    @Bean("workerChatModel")
    public ChatModel workerChatModel() {
        return OpenAiChatModel.builder()
                .openAiApi(createOpenAiApi())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("ag/gemini-3.7-flash-high")
                        .temperature(0.2)
                        .maxTokens(16000)
                        .build())
                .build();
    }

    @Bean("testerChatModel")
    public ChatModel testerChatModel() {
        return OpenAiChatModel.builder()
                .openAiApi(createOpenAiApi())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("ag/gemini-3.7-flash-high")
                        .temperature(0.2)
                        .maxTokens(12000)
                        .build())
                .build();
    }

    @Bean("helperChatModel")
    public ChatModel helperChatModel() {
        return OpenAiChatModel.builder()
                .openAiApi(createOpenAiApi())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("ag/gemini-3.6-flash-high")
                        .temperature(0.1)
                        .maxTokens(8000)
                        .build())
                .build();
    }
}