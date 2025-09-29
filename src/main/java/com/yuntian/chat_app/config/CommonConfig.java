package com.yuntian.chat_app.config;


import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CommonConfig {


    @Autowired
    private ChatMemoryStore redisChatMemoryStore;


    //构建chatMemoryProvider  用于提供会话记忆功能
    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        ChatMemoryProvider chatMemoryProvider = new ChatMemoryProvider(){
            @Override
            public ChatMemory get(Object memoryId){
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(redisChatMemoryStore) //配置chatMemoryStore，记忆存储
                        .build();
            }
        };
        return chatMemoryProvider;
    }


}
