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

    /**
     * 检索记忆
     * 🚀 修改：参数名从 userId 改为 memoryId，明确这是基于会话的检索
     */
    public String retrieveMemory(String memoryId, String userMessage) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(userMessage).content();

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .filter(metadataKey("memory_id").isEqualTo(memoryId)) // 👈 确保只查当前会话
                    .maxResults(3)
                    .minScore(0.6)
                    .build();

            List<EmbeddingMatch<TextSegment>> results = embeddingStore.search(request).matches();
            if (results.isEmpty()) {
                return "";
            }

            String memoryContext = results.stream()
                    .map(match -> match.embedded().text())
                    .collect(Collectors.joining("\n---\n"));

            log.info("🔍 RAG 命中会话记忆 (memoryId: {}): \n{}", memoryId, memoryContext);
            return memoryContext;
        } catch (Exception e) {
            log.error("向量检索失败", e);
            return "";
        }
    }

    /**
     * 存储记忆
     */
    @Async
    public void ingestMemory(String memoryId, String userMessage, String aiResponse, Long userId, Long characterId) {
        try {
            // 手动传递上下文给异步线程
            MonitorContext context = MonitorContext.builder()
                    .userId(String.valueOf(userId))
                    .characterId(String.valueOf(characterId))
                    .memoryId(memoryId)
                    .build();
            MonitorContextHolder.setContext(context);

            if (userMessage == null || aiResponse == null ||
                    userMessage.length() < 2 || aiResponse.length() < 2) {
                return;
            }

            String extracted = memoryWriterService.extract(userMessage, aiResponse);
            List<MemoryItem> items = parseMemoryItems(extracted);

            // 如果提取为空，说明 AI 觉得没啥好记的
            if (items.isEmpty()) {
                log.info("本次对话未提取到新记忆");
                return;
            }

            for (MemoryItem item : items) {
                if (item == null || item.memory == null || item.memory.isBlank()) continue;
                if (item.confidence == null || item.confidence < 0.7) continue;

                String memoryText = "【" + item.type.trim() + "】" + item.memory.trim();

                Embedding embedding = embeddingModel.embed(memoryText).content();

                // 查重：只在当前会话 memoryId 里查重
                if (isDuplicate(memoryId, embedding)) {
                    log.debug("记忆已存在，跳过: {}", memoryText);
                    continue;
                }

                // 入库：打上 memory_id 的标签
                Metadata metadata = new Metadata();
                metadata.put("memory_id", memoryId); // 👈 关键：存入会话ID
                metadata.put("memory_type", item.type.trim());
                // 也可以顺便存个 user_id 方便以后做数据迁移，但检索主要靠 memory_id
                metadata.put("user_id", String.valueOf(userId));

                TextSegment segment = TextSegment.from(memoryText, metadata);
                embeddingStore.add(embedding, segment);

                log.info("✅ 成功写入记忆: {} (Session: {})", memoryText, memoryId);
            }

        } catch (Exception e) {
            log.error("异步存储记忆失败", e);
        } finally {
            MonitorContextHolder.clearContext();
        }
    }

    private boolean isDuplicate(String memoryId, Embedding embedding) {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .filter(metadataKey("memory_id").isEqualTo(memoryId))
                .maxResults(1)
                .minScore(0.85)
                .build();
        List<EmbeddingMatch<TextSegment>> results = embeddingStore.search(request).matches();
        return !results.isEmpty();
    }

    private List<MemoryItem> parseMemoryItems(String raw) {
        if (raw == null) return Collections.emptyList();
        String text = raw.trim();
        // 简单的 JSON 提取逻辑
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < 0) return Collections.emptyList();

        try {
            String json = text.substring(start, end + 1);
            return objectMapper.readValue(json, new TypeReference<List<MemoryItem>>() {});
        } catch (Exception e) {
            log.warn("记忆解析失败: {}", raw);
            return Collections.emptyList();
        }
    }

    public static class MemoryItem {
        public String type;
        public String memory;
        public Double confidence;
    }
}