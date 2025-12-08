package com.yuntian.chat_app.listener;

import com.yuntian.chat_app.context.MonitorContext;
import com.yuntian.chat_app.context.MonitorContextHolder;
import com.yuntian.chat_app.service.userService.AiCallLogService;
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

    private final AiCallLogService aiCallLogService;

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
            String memoryId = context.getMemoryId();

/*            // 1. 记录成功请求
            metricsCollector.recordRequest(userId, characterId, modelName, "success");*/

            // 1. 记录响应时间
            Long startTime = (Long) attributes.get(REQUEST_START_TIME_KEY);
            long duration = 0;
            if (startTime != null) {
                duration = System.currentTimeMillis() - startTime;
                /*metricsCollector.recordResponseTime(userId, characterId, modelName, duration);*/
                log.info("AI 响应时间: {}ms", duration);
            }

            // 3. 记录 Token 使用情况
            TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
            Integer inputTokens = 0;
            Integer outputTokens = 0;
            Integer totalTokens = 0;
            if (tokenUsage != null) {
                inputTokens = tokenUsage.inputTokenCount();
                outputTokens = tokenUsage.outputTokenCount();
                totalTokens = tokenUsage.totalTokenCount();


                log.info("Token 使用情况 - input: {}, output: {}, total: {}",
                        inputTokens, outputTokens, totalTokens);
            } else {
                log.warn("响应中没有 Token 使用信息");
            }

            // 4. 异步保存调用记录
            aiCallLogService.saveCall(
                    userId, characterId, modelName, memoryId,
                    "success", inputTokens, outputTokens, totalTokens,
                    duration, startTime
            );

        } catch (Exception e) {
            log.error("记录 AI 响应信息失败", e);
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        try {
            Map<Object, Object> attributes = errorContext.attributes();
            MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

            if (context == null) {
                log.warn("无法获取监控上下文，跳过记录错误");
                return;
            }

            String userId = context.getUserId();
            String characterId = context.getCharacterId();
            String memoryId = context.getMemoryId();

            // 计算响应时间
            Long startTime = (Long) attributes.get(REQUEST_START_TIME_KEY);
            long duration = 0L;
            if (startTime != null) {
                duration = System.currentTimeMillis() - startTime;
            }

            // 记录失败请求到数据库
            aiCallLogService.saveCall(
                    userId,
                    characterId,
                    "unknown",
                    memoryId,
                    "error",
                    0,
                    0,
                    0,
                    duration,
                    startTime
            );

            log.error("AI 请求失败 - userId: {}, characterId: {}, error: {}",
                    userId, characterId, errorContext.error().getMessage(), errorContext.error());

        } catch (Exception e) {
            log.error("记录 AI 错误信息失败", e);
        }
    }

}
