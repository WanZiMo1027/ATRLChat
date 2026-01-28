package com.yuntian.chat_app.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * netty 与 前端之间传输消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupChatMessageDTO {
    private Integer type;          // 1: 加入群组, 2: 聊天消息, 3: 离开群组
    private Long messageId;        // 消息ID（服务端生成）
    private Long groupId;          // 群组ID (改为Long)
    private Long userId;           // 发送者ID (改为Long)
    private Long characterId;      // AI角色ID (改为Long)
    private String content;        // 消息内容
    private String contentType;    // 消息类型 (text/image/file)
    private String imageUrl;       // 图片URL
    private Long replyToId;        // 回复的消息ID
    private String senderName;     // 发送者名称
    private String senderAvatarUrl;// 发送者头像
    private String senderType;     // USER / AI
    private Long timestamp;        // 时间戳
}
