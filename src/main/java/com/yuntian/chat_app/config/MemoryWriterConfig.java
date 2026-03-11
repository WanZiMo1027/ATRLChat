
package com.yuntian.chat_app.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "memory-writer")
@Data
public class MemoryWriterConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int timeout;

    @Bean("memoryWriterChatModel")
    public OpenAiChatModel memoryWriterChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(1024)
                .timeout(Duration.ofSeconds(timeout))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
