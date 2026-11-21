package com.aicreative.aicreativeworkshop.ai.config;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Slf4j
@Configuration
public class OpenAiChatModelConfig {

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.max-tokens:8192}")
    private Integer maxTokens;

    @Value("${langchain4j.open-ai.chat-model.max-retries:3}")
    private Integer maxRetries;

    @Value("${langchain4j.open-ai.chat-model.log-requests:false}")
    private Boolean logRequests;

    @Value("${langchain4j.open-ai.chat-model.log-responses:false}")
    private Boolean logResponses;

    @Bean(name = "customOpenAiChatModel")
    @Primary
    public ChatModel openAiChatModel() {
        log.info("手动创建 openAiChatModel，使用 JdkHttpClient");
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .maxRetries(maxRetries)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.ofMinutes(5))
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }
}