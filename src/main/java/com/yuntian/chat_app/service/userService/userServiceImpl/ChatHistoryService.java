package com.yuntian.chat_app.service.userService.userServiceImpl;

import com.yuntian.chat_app.entity.PrivateChatMessage;
import com.yuntian.chat_app.mapper.userMapper.PrivateChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatHistoryService {

    private final StringRedisTemplate redisTemplate;
    private final PrivateChatMessageMapper privateChatMessageMapper;

    public List<Map<String, Object>> getHistoryList(Long userId, String characterId) {
        try {
            String listKey = "chat_sessions:" + userId + ":" + characterId;
            Set<String> sessionIds = redisTemplate.opsForZSet().range(listKey, 0, -1);

            if (sessionIds == null || sessionIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> historyList = new ArrayList<>();
            for (String sessionId : sessionIds) {
                try {
                    String sessionInfoKey = "session_info:" + sessionId;
                    Map<Object, Object> sessionInfo = redisTemplate.opsForHash().entries(sessionInfoKey);
                    if (sessionInfo.isEmpty()) {
                        continue;
                    }

                    Map<String, Object> sessionItem = new HashMap<>();
                    sessionItem.put("sessionId", sessionId);
                    sessionItem.put("createTime", sessionInfo.get("createTime"));
                    sessionItem.put("updateTime", sessionInfo.get("updateTime"));
                    sessionItem.put("lastMessage", getLastMessagePreview(sessionId));
                    historyList.add(sessionItem);
                } catch (Exception e) {
                    log.warn("获取会话信息失败: {}", sessionId, e);
                }
            }

            historyList.sort((a, b) -> {
                String timeA = String.valueOf(a.get("updateTime"));
                String timeB = String.valueOf(b.get("updateTime"));
                return timeB.compareTo(timeA);
            });

            return historyList;
        } catch (Exception e) {
            log.error("获取历史会话列表失败", e);
            return new ArrayList<>();
        }
    }

    public String createNewSession(Long userId, String characterId) {
        long timestamp = System.currentTimeMillis();
        String sessionId = "chat_" + userId + "_" + characterId + "_" + timestamp;

        try {
            String listKey = "chat_sessions:" + userId + ":" + characterId;
            redisTemplate.opsForZSet().add(listKey, sessionId, timestamp);

            String sessionInfoKey = "session_info:" + sessionId;
            Map<String, String> sessionInfo = new HashMap<>();
            sessionInfo.put("userId", userId.toString());
            sessionInfo.put("characterId", characterId);
            sessionInfo.put("createTime", String.valueOf(timestamp));
            sessionInfo.put("updateTime", String.valueOf(timestamp));
            redisTemplate.opsForHash().putAll(sessionInfoKey, sessionInfo);

            redisTemplate.expire(listKey, 30, TimeUnit.DAYS);
            redisTemplate.expire(sessionInfoKey, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("创建会话记录失败", e);
        }

        return sessionId;
    }

    public void updateSessionActivity(String sessionId) {
        try {
            long currentTime = System.currentTimeMillis();
            String sessionInfoKey = "session_info:" + sessionId;
            if (redisTemplate.hasKey(sessionInfoKey)) {
                redisTemplate.opsForHash().put(sessionInfoKey, "updateTime", String.valueOf(currentTime));
            }
        } catch (Exception e) {
            log.warn("更新会话活动时间失败: {}", sessionId, e);
        }
    }

    public void ensureSessionTracked(Long userId, String characterId, String sessionId) {
        try {
            String listKey = "chat_sessions:" + userId + ":" + characterId;
            String sessionInfoKey = "session_info:" + sessionId;
            Double score = redisTemplate.opsForZSet().score(listKey, sessionId);

            if (score == null) {
                long currentTime = System.currentTimeMillis();
                redisTemplate.opsForZSet().add(listKey, sessionId, currentTime);

                Map<String, String> sessionInfo = new HashMap<>();
                sessionInfo.put("userId", userId.toString());
                sessionInfo.put("characterId", characterId);
                sessionInfo.put("createTime", String.valueOf(currentTime));
                sessionInfo.put("updateTime", String.valueOf(currentTime));
                redisTemplate.opsForHash().putAll(sessionInfoKey, sessionInfo);

                redisTemplate.expire(listKey, 30, TimeUnit.DAYS);
                redisTemplate.expire(sessionInfoKey, 30, TimeUnit.DAYS);

                log.info("会话已自动添加到跟踪列表: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("确保会话跟踪失败: {}", sessionId, e);
        }
    }

    private String getLastMessagePreview(String sessionId) {
        try {
            PrivateChatMessage latestMessage = privateChatMessageMapper.selectLatestByMemoryId(sessionId);
            if (latestMessage == null) {
                return "暂无消息";
            }

            String content = latestMessage.getContent();
            if (content != null && !content.isBlank()) {
                return content.length() > 50 ? content.substring(0, 50) + "..." : content;
            }

            if (latestMessage.getImageUrl() != null && !latestMessage.getImageUrl().isBlank()) {
                return "[图片]";
            }
        } catch (Exception e) {
            log.warn("获取最后消息预览失败: {}", sessionId, e);
        }
        return "暂无消息";
    }
}
