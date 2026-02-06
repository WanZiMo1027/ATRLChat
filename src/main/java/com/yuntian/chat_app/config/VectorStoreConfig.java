package com.yuntian.chat_app.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 向量数据库配置 (仅供 RAG 使用)
@Configuration
public class VectorStoreConfig {

    @Value("${rag.postgres.host}")
    private String host;
    @Value("${rag.postgres.port}")
    private Integer port;
    @Value("${rag.postgres.database}")
    private String database;
    @Value("${rag.postgres.user}")
    private String user;
    @Value("${rag.postgres.password}")
    private String password;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 这就是专门管理冷存储的“档案管理员”
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table("conversation_memories")
                .dimension(1024)
                .build();
    }
}