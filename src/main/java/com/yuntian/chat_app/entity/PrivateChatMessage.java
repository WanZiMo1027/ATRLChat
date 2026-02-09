package com.yuntian.chat_app.entity;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PrivateChatMessage {
    private Long id;
    private String memoryId;      // 核心：用来区分不同对话窗口
    private Long userId;          // 实际上是当前登录用户
    private Long characterId;     // 聊天的对象（AI角色）
    private String senderType;    // "USER" 或 "AI"
    private String content;
    private String imageUrl;
    private Integer isDeleted;
    private LocalDateTime createTime;
}