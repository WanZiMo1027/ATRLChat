package com.yuntian.chat_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(excludeName = {
        "dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration"
})
@EnableAsync
public class ChatAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatAppApplication.class, args);
    }

}
