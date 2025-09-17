package com.yuntian.chat_app.repository;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RedisChatMemoryStore implements ChatMemoryStore {

    // 注入redisTemplate
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        //获取会话消息
        //1.从redis中获取json
        String json = redisTemplate.opsForValue().get(memoryId);

        // 如果json为空或者只包含空白字符，返回空列表
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 清理json中的控制字符
        json = cleanJsonString(json);

        try {
            //2.把json转化为list
            List<ChatMessage> list = ChatMessageDeserializer.messagesFromJson(json);
            return list;
        } catch (Exception e) {
            // 如果解析失败，记录错误并返回空列表
            System.err.println("Failed to deserialize messages for memoryId: " + memoryId);
            e.printStackTrace();
            // 删除损坏的数据
            redisTemplate.delete(memoryId.toString());
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        //更新会话消息
        //1.把list转化为json
        String json = ChatMessageSerializer.messagesToJson(list);

        // 确保json不包含控制字符
        json = cleanJsonString(json);

        //2.把json存储到redis中，设置7天过期时间
        redisTemplate.opsForValue().set(memoryId.toString(), json, Duration.ofDays(7));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        //删除会话消息
        redisTemplate.delete(memoryId.toString());
    }

    /**
     * 清理JSON字符串中的控制字符
     */
    private String cleanJsonString(String json) {
        if (json == null) {
            return null;
        }
        // 移除所有控制字符（0-31，除了\t \n \r）
        return json.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }
}

