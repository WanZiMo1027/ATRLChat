package com.yuntian.chat_app.config;


import com.yuntian.chat_app.listener.AiModelMetricsListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class LangChain4jConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Boolean logRequests;
    private Boolean logResponses;

    @Resource
    private AiModelMetricsListener aiModelMetricsListener;

    @Bean("customOpenAiChatModel")
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(4096)
                .timeout(Duration.ofSeconds(60))
                .logRequests(logRequests)
                .logResponses(logResponses)
                .listeners(List.of(aiModelMetricsListener))
                .build();
    }
}
