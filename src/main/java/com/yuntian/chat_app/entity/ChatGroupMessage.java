package com.yuntian.chat_app.entity;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ChatGroupMessage {

    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderType;  // USER, AI
    private Long characterId;
    private String content;
    private String contentType;
    private String imageUrl;
    private Long replyToId;
    private LocalDateTime createTime;
    private Integer isDeleted;
}
