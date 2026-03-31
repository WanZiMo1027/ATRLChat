package com.yuntian.chat_app.service.userService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuntian.chat_app.context.MonitorContext;
import com.yuntian.chat_app.context.MonitorContextHolder;
import com.yuntian.chat_app.entity.MemoryIndexDO;
import com.yuntian.chat_app.mapper.userMapper.MemoryIndexMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final ObjectMapper objectMapper;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final MemoryWriterService memoryWriterService;
    private final MemoryIndexMapper memoryIndexMapper;

    private volatile Boolean explicitEmbeddingIdSupported;

    /**
     * 检索记忆（继续使用向量相似度做模糊检索）
     */
    public String retrieveMemory(String memoryId, String userMessage) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(userMessage).content();

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .filter(metadataKey("memory_id").isEqualTo(memoryId))
                    .maxResults(3)
                    .minScore(0.75)
                    .build();

            List<EmbeddingMatch<TextSegment>> results = embeddingStore.search(request).matches();
            if (results.isEmpty()) {
                return "";
            }

            return results.stream()
                    .map(match -> match.embedded().text())
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("向量检索失败", e);
            return "";
        }
    }

    /**
     * 写入记忆（按 memoryId + key 覆盖）
     */
    @Async
    public void ingestMemory(String memoryId, String userMessage, String aiResponse, Long userId, Long characterId) {
        try {
            MonitorContext context = MonitorContext.builder()
                    .userId(String.valueOf(userId))
                    .characterId(String.valueOf(characterId))
                    .memoryId(memoryId)
                    .build();
            MonitorContextHolder.setContext(context);

            String extracted = memoryWriterService.extract(userMessage, aiResponse);
            List<MemoryItem> items = parseMemoryItems(extracted);
            if (items.isEmpty()) {
                return;
            }

            for (MemoryItem item : items) {
                if (item.memory == null || item.key == null || item.confidence == null || item.confidence < 0.7) {
                    continue;
                }

                String memoryText = "【" + item.type + "】" + item.memory;
                upsertMemory(memoryId, item.key, memoryText, userId);
            }
        } catch (Exception e) {
            log.error("异步存储记忆失败", e);
        } finally {
            MonitorContextHolder.clearContext();
        }
    }

    private void upsertMemory(String memoryId, String key, String memoryText, Long userId) {
        Embedding embedding = embeddingModel.embed(memoryText).content();
        Metadata metadata = new Metadata();
        metadata.put("memory_id", memoryId);
        metadata.put("memory_key", key);
        metadata.put("user_id", String.valueOf(userId));
        metadata.put("timestamp", System.currentTimeMillis());

        TextSegment segment = TextSegment.from(memoryText, metadata);
        String fixedEmbeddingId = buildEmbeddingId(memoryId, key);

        if (supportsExplicitEmbeddingId()) {
            try {
                embeddingStore.remove(fixedEmbeddingId);
                invokeExplicitIdAdd(fixedEmbeddingId, embedding, segment);
                log.info("✅ 成功存入记忆: {} (key={}, embeddingId={})", memoryText, key, fixedEmbeddingId);
                return;
            } catch (Exception e) {
                explicitEmbeddingIdSupported = false;
                log.warn("显式 embeddingId 写入失败，降级为索引表模式 [embeddingId={}]", fixedEmbeddingId, e);
            }
        }

        MemoryIndexDO existing = memoryIndexMapper.findByMemoryIdAndKey(memoryId, key);
        Set<String> idsToRemove = new LinkedHashSet<>();
        idsToRemove.add(fixedEmbeddingId);
        if (existing != null && existing.getEmbeddingId() != null && !existing.getEmbeddingId().isBlank()) {
            idsToRemove.add(existing.getEmbeddingId());
        }

        for (String embeddingId : idsToRemove) {
            removeEmbeddingQuietly(embeddingId);
        }

        String generatedEmbeddingId = invokeGeneratedIdAdd(embedding, segment);
        memoryIndexMapper.upsert(memoryId, key, generatedEmbeddingId);
        log.info("✅ 成功存入记忆: {} (key={}, embeddingId={})", memoryText, key, generatedEmbeddingId);
    }

    private String buildEmbeddingId(String memoryId, String key) {
        return "memory::" + memoryId + "::" + key;
    }

    private boolean supportsExplicitEmbeddingId() {
        Boolean cached = explicitEmbeddingIdSupported;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (explicitEmbeddingIdSupported == null) {
                explicitEmbeddingIdSupported = findExplicitAddMethod() != null;
                log.info("EmbeddingStore 显式 embeddingId 支持: {}", explicitEmbeddingIdSupported);
            }
            return explicitEmbeddingIdSupported;
        }
    }

    private void invokeExplicitIdAdd(String embeddingId, Embedding embedding, TextSegment segment) throws Exception {
        Method method = findExplicitAddMethod();
        if (method == null) {
            throw new IllegalStateException("当前 EmbeddingStore 不支持显式 embeddingId 写入");
        }
        method.invoke(embeddingStore, embeddingId, embedding, segment);
    }

    private String invokeGeneratedIdAdd(Embedding embedding, TextSegment segment) {
        try {
            Method method = findGeneratedAddMethod();
            if (method == null) {
                throw new IllegalStateException("未找到 EmbeddingStore.add(Embedding, TextSegment) 方法");
            }

            Object result = method.invoke(embeddingStore, embedding, segment);
            if (result instanceof String generatedId && !generatedId.isBlank()) {
                return generatedId;
            }
            throw new IllegalStateException("EmbeddingStore 未返回可用的 embeddingId");
        } catch (Exception e) {
            throw new IllegalStateException("写入向量记忆失败", e);
        }
    }

    private Method findExplicitAddMethod() {
        for (Method method : embeddingStore.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!"add".equals(method.getName()) || parameterTypes.length != 3) {
                continue;
            }
            if (parameterTypes[0] != String.class) {
                continue;
            }
            if (!parameterTypes[1].isAssignableFrom(Embedding.class)) {
                continue;
            }
            if (!parameterTypes[2].isAssignableFrom(TextSegment.class)) {
                continue;
            }
            return method;
        }
        return null;
    }

    private Method findGeneratedAddMethod() {
        for (Method method : embeddingStore.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!"add".equals(method.getName()) || parameterTypes.length != 2) {
                continue;
            }
            if (!parameterTypes[0].isAssignableFrom(Embedding.class)) {
                continue;
            }
            if (!parameterTypes[1].isAssignableFrom(TextSegment.class)) {
                continue;
            }
            return method;
        }
        return null;
    }

    private void removeEmbeddingQuietly(String embeddingId) {
        if (embeddingId == null || embeddingId.isBlank()) {
            return;
        }
        try {
            embeddingStore.remove(embeddingId);
        } catch (Exception e) {
            log.debug("删除旧记忆失败，继续覆盖流程 [embeddingId={}]", embeddingId, e);
        }
    }

    private List<MemoryItem> parseMemoryItems(String raw) {
        try {
            int start = raw.indexOf('[');
            int end = raw.lastIndexOf(']');
            if (start < 0 || end < 0) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(raw.substring(start, end + 1), new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("解析记忆 JSON 失败: {}", raw);
            return Collections.emptyList();
        }
    }

    public static class MemoryItem {
        public String type;
        public String key;
        public String memory;
        public Double confidence;
    }
}
