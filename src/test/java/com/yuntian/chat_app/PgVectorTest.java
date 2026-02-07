package com.yuntian.chat_app;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@SpringBootTest
// 🔥 新增这行：强制在测试环境中禁用 Netty，防止端口冲突
@TestPropertySource(properties = "chatapp.netty.websocket.enabled=false")
class PgVectorTest {

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void testMemoryStorageAndRetrieval() {

        // ==========================================
        // 镜头 1：存入记忆 (Write)
        // ==========================================
        String userId = "test_user_007";
        String memoryContent = "用户说：我叫张三，是一名喜欢喝冰美式的Java程序员。";

        // 1. 准备数据片段 (TextSegment)
        // 修复点：使用 Metadata.from() 或 .put()
        Metadata metadata = new Metadata().put("user_id", userId);
        TextSegment segment = TextSegment.from(memoryContent, metadata);

        // 2. 调用模型生成向量
        System.out.println("⏳ 正在生成向量 (调用 text-embedding-v4)...");
        Embedding embedding = embeddingModel.embed(segment).content();
        System.out.println("✅ 向量生成成功，维度：" + embedding.dimension());

        // 3. 存入 PostgreSQL
        embeddingStore.add(embedding, segment);
        System.out.println("✅ 记忆已存入数据库表 conversation_memories");

        // ==========================================
        // 镜头 2：检索记忆 (Read)
        // ==========================================
        String query = "我是谁？";
        System.out.println("\n🔎 正在检索问题：" + query);

        // 1. 将问题向量化
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 2. 在数据库中搜索
        // 修复点：使用 EmbeddingSearchRequest 构建请求
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .minScore(0.6)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> results = result.matches();

        // ==========================================
        // 镜头 3：验证结果
        // ==========================================
        if (results.isEmpty()) {
            System.err.println("❌ 测试失败：未找到相关记忆！");
        } else {
            EmbeddingMatch<TextSegment> match = results.get(0);
            System.out.println("🎉 测试通过！检索结果如下：");
            System.out.println("--------------------------------------------------");
            System.out.println("相似度分数: " + match.score());
            System.out.println("记忆内容: " + match.embedded().text());
            System.out.println("元数据: " + match.embedded().metadata());
            System.out.println("--------------------------------------------------");
        }
    }
}