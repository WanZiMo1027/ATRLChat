package com.yuntian.chat_app.service.userService.userServiceImpl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;


import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatHistoryService {

    @Autowired
    private ChatMemoryProvider chatMemoryProvider;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取指定会话ID的聊天记录
     * @param memoryId 会话ID
     * @return 聊天记录列表，每个元素包含消息类型(type)和内容(content)
     */
    public List<Map<String, String>> getChatHistory(String memoryId) {
        ChatMemory chatMemory = chatMemoryProvider.get(memoryId);
        List<ChatMessage> messages = chatMemory.messages();

        return messages.stream()
                .filter(msg -> msg.type() == ChatMessageType.USER || msg.type() == ChatMessageType.AI)
                .map(msg -> {
                    Map<String, String> chatItem = new HashMap<>();
                    chatItem.put("type", msg.type() == ChatMessageType.USER ? "user" : "ai");

                    // 根据消息类型获取内容
                    String content = "";
                    if (msg instanceof UserMessage) {
                        content = ((UserMessage) msg).singleText();
                    } else if (msg instanceof AiMessage) {
                        content = ((AiMessage) msg).text();
                    }

                    chatItem.put("content", content);
                    return chatItem;
                })
                .filter(chatItem -> chatItem.get("content") != null
                        && !chatItem.get("content").trim().isEmpty())
                .collect(Collectors.toList());
    }


    /**
     * 获取用户与指定角色的所有会话列表
     */
    public List<Map<String, Object>> getHistoryList(Long userId, String characterId) {
        try {
            // Redis中存储会话列表的key
            String listKey = "chat_sessions:" + userId + ":" + characterId;

            // 获取所有会话ID
            Set<String> sessionIds = redisTemplate.opsForZSet().range(listKey, 0, -1);

            if (sessionIds == null || sessionIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> historyList = new ArrayList<>();

            for (String sessionId : sessionIds) {
                try {
                    // 获取会话的基本信息
                    String sessionInfoKey = "session_info:" + sessionId;
                    Map<Object, Object> sessionInfo = redisTemplate.opsForHash().entries(sessionInfoKey);

                    if (!sessionInfo.isEmpty()) {
                        Map<String, Object> sessionItem = new HashMap<>();
                        sessionItem.put("sessionId", sessionId);
                        sessionItem.put("createTime", sessionInfo.get("createTime"));
                        sessionItem.put("updateTime", sessionInfo.get("updateTime"));

                        // 获取最后一条消息作为预览
                        String lastMessage = getLastMessagePreview(sessionId);
                        sessionItem.put("lastMessage", lastMessage);

                        historyList.add(sessionItem);
                    }
                } catch (Exception e) {
                    log.warn("获取会话信息失败: {}", sessionId, e);
                }
            }

            // 按更新时间倒序排列
            historyList.sort((a, b) -> {
                String timeA = (String) a.get("updateTime");
                String timeB = (String) b.get("updateTime");
                return timeB.compareTo(timeA);
            });

            return historyList;

        } catch (Exception e) {
            log.error("获取历史会话列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 创建新的聊天会话并记录到Redis
     */
    public String createNewSession(Long userId, String characterId) {
        // 生成新的sessionId
        long timestamp = System.currentTimeMillis();
        String sessionId = "chat_" + userId + "_" + characterId + "_" + timestamp;

        try {
            // 1. 将会话ID添加到用户-角色的会话列表中（使用ZSet，score为创建时间）
            String listKey = "chat_sessions:" + userId + ":" + characterId;
            redisTemplate.opsForZSet().add(listKey, sessionId, timestamp);

            // 2. 存储会话的详细信息
            String sessionInfoKey = "session_info:" + sessionId;
            Map<String, String> sessionInfo = new HashMap<>();
            sessionInfo.put("userId", userId.toString());
            sessionInfo.put("characterId", characterId);
            sessionInfo.put("createTime", String.valueOf(timestamp));
            sessionInfo.put("updateTime", String.valueOf(timestamp));

            redisTemplate.opsForHash().putAll(sessionInfoKey, sessionInfo);

            // 设置过期时间（30天）
            redisTemplate.expire(listKey, 30, TimeUnit.DAYS);
            redisTemplate.expire(sessionInfoKey, 30, TimeUnit.DAYS);

        } catch (Exception e) {
            log.error("创建会话记录失败", e);
        }

        return sessionId;
    }

    /**
     * 更新会话的最后活动时间（每次聊天时调用）
     */
    public void updateSessionActivity(String sessionId) {
        try {
            long currentTime = System.currentTimeMillis();
            String sessionInfoKey = "session_info:" + sessionId;

            // 检查会话是否存在
            if (redisTemplate.hasKey(sessionInfoKey)) {
                redisTemplate.opsForHash().put(sessionInfoKey, "updateTime", String.valueOf(currentTime));
            }
        } catch (Exception e) {
            log.warn("更新会话活动时间失败: {}", sessionId, e);
        }
    }

    /**
     * 获取会话的最后一条消息作为预览
     */
    private String getLastMessagePreview(String sessionId) {
        try {
            List<Map<String, String>> messages = getChatHistory(sessionId);
            if (!messages.isEmpty()) {
                Map<String, String> lastMsg = messages.get(messages.size() - 1);
                String content = lastMsg.get("content");
                // 限制预览长度
                return content != null && content.length() > 50 ?
                        content.substring(0, 50) + "..." : content;
            }
        } catch (Exception e) {
            log.warn("获取最后消息预览失败: {}", sessionId, e);
        }
        return "暂无消息";
    }
    /**
     * 确保会话被跟踪到Redis会话列表中 - 关键方法
     */
    public void ensureSessionTracked(Long userId, String characterId, String sessionId) {
        try {
            String listKey = "chat_sessions:" + userId + ":" + characterId;
            String sessionInfoKey = "session_info:" + sessionId;

            // 检查会话是否已经在列表中
            Double score = redisTemplate.opsForZSet().score(listKey, sessionId);

            if (score == null) {
                // 会话不在列表中，需要添加
                long currentTime = System.currentTimeMillis();

                // 1. 添加到会话列表（使用当前时间作为score）
                redisTemplate.opsForZSet().add(listKey, sessionId, currentTime);

                // 2. 创建会话信息
                Map<String, String> sessionInfo = new HashMap<>();
                sessionInfo.put("userId", userId.toString());
                sessionInfo.put("characterId", characterId);
                sessionInfo.put("createTime", String.valueOf(currentTime));
                sessionInfo.put("updateTime", String.valueOf(currentTime));



                redisTemplate.opsForHash().putAll(sessionInfoKey, sessionInfo);

                // 设置过期时间
                redisTemplate.expire(listKey, 30, TimeUnit.DAYS);
                redisTemplate.expire(sessionInfoKey, 30, TimeUnit.DAYS);

                log.info("会话已自动添加到跟踪列表: {}", sessionId);
            }

        } catch (Exception e) {
            log.error("确保会话跟踪失败: {}", sessionId, e);
        }
    }
}

