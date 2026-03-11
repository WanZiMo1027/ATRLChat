package com.yuntian.chat_app.service.userService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuntian.chat_app.context.MonitorContext;
import com.yuntian.chat_app.context.MonitorContextHolder;
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

import java.util.Collections;
import java.util.List;
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

    // 根据 text-embedding-v4 的规格设定维度 (1024)
    private static final int EMBEDDING_DIMENSION = 1024;

    /**
     * 检索记忆（依然使用向量相似度实现模糊检索）
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
            if (results.isEmpty()) return "";

            return results.stream()
                    .map(match -> match.embedded().text())
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("向量检索失败", e);
            return "";
        }
    }

    /**
     * 写入记忆（核心逻辑：按 key 覆盖）
     */
    @Async
    public void ingestMemory(String memoryId, String userMessage, String aiResponse, Long userId, Long characterId) {
        try {
            MonitorContext context = MonitorContext.builder()
                    .userId(String.valueOf(userId)).characterId(String.valueOf(characterId)).memoryId(memoryId).build();
            MonitorContextHolder.setContext(context);

            String extracted = memoryWriterService.extract(userMessage, aiResponse);
            List<MemoryItem> items = parseMemoryItems(extracted);
            if (items.isEmpty()) return;

            for (MemoryItem item : items) {
                if (item.memory == null || item.key == null || item.confidence < 0.7) continue;

                String memoryText = "【" + item.type + "】" + item.memory;

                // 🔥 1. 精确查找同 key 的旧记忆（零 Token 消耗）
                List<EmbeddingMatch<TextSegment>> existing = findByKey(memoryId, item.key);
                if (!existing.isEmpty()) {
                    existing.forEach(match -> {
                        log.info("🔄 覆盖旧记忆 [key={}]: {} -> {}", item.key, match.embedded().text(), memoryText);
                        embeddingStore.remove(match.embeddingId());
                    });
                }

                // 🔥 2. 存入新记忆（依然需要 Embedding 给未来的检索使用）
                Embedding embedding = embeddingModel.embed(memoryText).content();
                Metadata metadata = new Metadata();
                metadata.put("memory_id", memoryId);
                metadata.put("memory_key", item.key); // 存储唯一标识
                metadata.put("user_id", String.valueOf(userId));
                metadata.put("timestamp", System.currentTimeMillis());

                embeddingStore.add(embedding, TextSegment.from(memoryText, metadata));
                log.info("✅ 成功存入记忆: {} (key={})", memoryText, item.key);
            }
        } catch (Exception e) {
            log.error("异步存储记忆失败", e);
        } finally {
            MonitorContextHolder.clearContext();
        }
    }

    /**
     * 按 Key 精确过滤：手动创建合法伪向量，强制依靠 metadata filter 命中
     */
    private List<EmbeddingMatch<TextSegment>> findByKey(String memoryId, String key) {
        float[] dummyArray = new float[EMBEDDING_DIMENSION];
        dummyArray[0] = 1.0f; // 🔥 修复点 1：给第一维赋个值，避免全0向量导致的数学“除以0”错误
        Embedding dummyEmbedding = new Embedding(dummyArray);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(dummyEmbedding)
                .filter(metadataKey("memory_id").isEqualTo(memoryId)
                        .and(metadataKey("memory_key").isEqualTo(key)))
                .maxResults(5)
                .minScore(0.0)
                .build();

        return embeddingStore.search(request).matches();
    }

    private List<MemoryItem> parseMemoryItems(String raw) {
        try {
            int start = raw.indexOf('[');
            int end = raw.lastIndexOf(']');
            if (start < 0 || end < 0) return Collections.emptyList();
            return objectMapper.readValue(raw.substring(start, end + 1), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析记忆 JSON 失败: {}", raw);
            return Collections.emptyList();
        }
    }

    public static class MemoryItem {
        public String type;
        public String key; // 👈 新增
        public String memory;
        public Double confidence;
    }
}