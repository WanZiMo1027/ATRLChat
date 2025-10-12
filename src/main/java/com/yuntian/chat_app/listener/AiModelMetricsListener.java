package com.yuntian.chat_app.listener;

import com.yuntian.chat_app.context.MonitorContext;
import com.yuntian.chat_app.context.MonitorContextHolder;
import com.yuntian.chat_app.metrics.AiModelMetricsCollector;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AiModelMetricsListener implements ChatModelListener{

    private static final String MONITOR_CONTEXT_KEY = "monitorContext";
    private static final String REQUEST_START_TIME_KEY = "requestStartTime";

    private final AiModelMetricsCollector metricsCollector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        try {
            // 从 ThreadLocal 获取监控上下文
            MonitorContext context = MonitorContextHolder.getContext();
            if (context == null) {
                log.warn("监控上下文为空，跳过记录");
                return;
            }

            // 存储到 attributes（请求和响应共享）
            Map<Object, Object> attributes = requestContext.attributes();
            attributes.put(MONITOR_CONTEXT_KEY, context);
            attributes.put(REQUEST_START_TIME_KEY, System.currentTimeMillis());

            // 获取模型名称
            String modelName = requestContext.chatRequest().modelName();
            log.info("AI 请求开始 - userId: {}, characterId: {}, model: {}, memoryId: {}",
                    context.getUserId(), context.getCharacterId(), modelName, context.getMemoryId());

        } catch (Exception e) {
            log.error("记录 AI 请求信息失败", e);
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        try {
            // 从 attributes 获取监控上下文
            Map<Object, Object> attributes = responseContext.attributes();
            MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

            if (context == null) {
                log.warn("无法获取监控上下文，跳过记录");
                return;
            }

            String userId = context.getUserId();
            String characterId = context.getCharacterId();
            String modelName = responseContext.chatResponse().modelName();

            // 1. 记录成功请求
            metricsCollector.recordRequest(userId, characterId, modelName, "success");

            // 2. 记录响应时间
            Long startTime = (Long) attributes.get(REQUEST_START_TIME_KEY);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                metricsCollector.recordResponseTime(userId, characterId, modelName, duration);
                log.info("AI 响应时间: {}ms", duration);
            }

            // 3. 记录 Token 使用情况
            TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
            if (tokenUsage != null) {
                Integer inputTokens = tokenUsage.inputTokenCount();
                Integer outputTokens = tokenUsage.outputTokenCount();
                Integer totalTokens = tokenUsage.totalTokenCount();

                metricsCollector.recordTokenUsage(userId, characterId, modelName, "input", inputTokens);
                metricsCollector.recordTokenUsage(userId, characterId, modelName, "output", outputTokens);
                metricsCollector.recordTokenUsage(userId, characterId, modelName, "total", totalTokens);

                log.info("Token 使用情况 - input: {}, output: {}, total: {}",
                        inputTokens, outputTokens, totalTokens);
            } else {
                log.warn("响应中没有 Token 使用信息");
            }

        } catch (Exception e) {
            log.error("记录 AI 响应信息失败", e);
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        try {
            Map<Object, Object> attributes = errorContext.attributes();
            MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

            if (context != null) {
                // 记录失败请求
                metricsCollector.recordRequest(
                        context.getUserId(),
                        context.getCharacterId(),
                        "unknown",
                        "error"
                );

                log.error("AI 请求失败 - userId: {}, characterId: {}, error: {}",
                        context.getUserId(), context.getCharacterId(),
                        errorContext.error().getMessage(), errorContext.error());
            }
        } catch (Exception e) {
            log.error("记录 AI 错误信息失败", e);
        }
    }

}
