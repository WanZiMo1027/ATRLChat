package com.yuntian.chat_app.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AiModelMetricsCollector {
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();
    private final Map<String, Counter> requestCountersCache = new ConcurrentHashMap<>();
    private final Map<String, Timer> timersCache = new ConcurrentHashMap<>();

    public AiModelMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录 Token 消耗
     */
    public void recordTokenUsage(String userId, String characterId, String modelName,
                                 String tokenType, Integer tokenCount) {
        if (tokenCount == null || tokenCount <= 0) {
            return;
        }

        String key = String.format("%s_%s_%s_%s", userId, characterId, modelName, tokenType);
        Counter counter = tokenCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_character_tokens_total")
                        .description("AI 角色对话 Token 消耗总数")
                        .tag("user_id", userId)
                        .tag("character_id", characterId)
                        .tag("model_name", modelName)
                        .tag("token_type", tokenType)
                        .register(meterRegistry)
        );
        counter.increment(tokenCount);
    }

    /**
     * 记录请求次数
     */
    public void recordRequest(String userId, String characterId, String modelName, String status) {
        String key = String.format("%s_%s_%s_%s", userId, characterId, modelName, status);
        Counter counter = requestCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_character_requests_total")
                        .description("AI 角色对话请求总数")
                        .tag("user_id", userId)
                        .tag("character_id", characterId)
                        .tag("model_name", modelName)
                        .tag("status", status)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录响应时间
     */
    public void recordResponseTime(String userId, String characterId, String modelName, long duration) {
        String key = String.format("%s_%s_%s", userId, characterId, modelName);
        Timer timer = timersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_character_response_time")
                        .description("AI 角色对话响应时间")
                        .tag("user_id", userId)
                        .tag("character_id", characterId)
                        .tag("model_name", modelName)
                        .register(meterRegistry)
        );
        timer.record(duration, TimeUnit.MILLISECONDS);
    }
}
