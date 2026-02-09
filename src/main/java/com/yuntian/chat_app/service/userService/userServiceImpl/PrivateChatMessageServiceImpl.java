package com.yuntian.chat_app.service.userService.userServiceImpl;

import com.yuntian.chat_app.entity.PrivateChatMessage;
import com.yuntian.chat_app.mapper.userMapper.PrivateChatMessageMapper;
import com.yuntian.chat_app.service.userService.PrivateChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivateChatMessageServiceImpl implements PrivateChatMessageService {

    private final PrivateChatMessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(String memoryId, Long userId, Long characterId, String senderType, String content, String imageUrl) {
        PrivateChatMessage msg = new PrivateChatMessage();
        msg.setMemoryId(memoryId);
        msg.setUserId(userId);
        msg.setCharacterId(characterId);
        msg.setSenderType(senderType); // "USER" 或 "AI"
        msg.setContent(content);
        msg.setImageUrl(imageUrl);
        // createTime 数据库会自动生成，或者这里 set 也行
        messageMapper.insert(msg);
    }

    @Override
    public List<Map<String, String>> getHistory(String memoryId, int page, int size) {
        int offset = (page - 1) * size;
        List<PrivateChatMessage> entities = messageMapper.selectByMemoryId(memoryId, offset, size);

        // 转换为前端需要的简单格式 { type: "user", content: "..." }
        return entities.stream().map(entity -> {
            Map<String, String> chatItem = new HashMap<>();
            // 统一转换类型标识：数据库存 USER/AI -> 前端用 user/ai
            chatItem.put("type", "USER".equalsIgnoreCase(entity.getSenderType()) ? "user" : "ai");

            // 简单处理：如果是图片，可以拼接到 content 或者单独字段
            // 这里为了兼容旧前端逻辑，如果只有图片，content可能会显示特殊文本
            String finalContent = entity.getContent();
            if (entity.getImageUrl() != null && !entity.getImageUrl().isEmpty()) {
                // 如果你想在内容里体现图片
                // finalContent += " [图片]";
                // 或者前端如果有 imageUrl 字段更好
                chatItem.put("imageUrl", entity.getImageUrl());
            }
            chatItem.put("content", finalContent);
            return chatItem;
        }).collect(Collectors.toList());
    }
}