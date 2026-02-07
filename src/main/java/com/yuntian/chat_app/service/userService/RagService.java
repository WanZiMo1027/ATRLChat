package com.yuntian.chat_app.service.userService;

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

import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public String retrieveMemory(String userId, String userMessage) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(userMessage).content();

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .filter(metadataKey("user_id").isEqualTo(userId))
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

            log.info("🔍 RAG 命中记忆 (UserId: {}): \n{}", userId, memoryContext);
            return memoryContext;
        } catch (Exception e) {
            log.error("向量检索失败，降级为无记忆模式", e);
            return "";
        }
    }

    @Async
    public void ingestMemory(String userId, String userMessage, String aiResponse) {
        try {
            if (userMessage == null || aiResponse == null) {
                return;
            }
            if (userMessage.length() < 2 || aiResponse.length() < 2) {
                return;
            }

            String memoryText = "用户：" + userMessage + "\nAI：" + aiResponse;

            Metadata metadata = new Metadata();
            metadata.put("user_id", userId);
            TextSegment segment = TextSegment.from(memoryText, metadata);

            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);

            log.debug("已异步存储长期记忆");
        } catch (Exception e) {
            log.error("异步存储记忆失败", e);
        }
    }
}

