package com.yuntian.chat_app.service.userService;

import java.util.List;
import java.util.Map;

public interface PrivateChatMessageService {

    /**
     * 保存一条消息 (用户 或 AI)
     */
    void saveMessage(String memoryId, Long userId, Long characterId, String senderType, String content, String imageUrl);

    /**
     * 分页获取会话历史
     * 返回前端需要的格式 (List<Map>)
     */
    List<Map<String, String>> getHistory(String memoryId, int page, int size);
}