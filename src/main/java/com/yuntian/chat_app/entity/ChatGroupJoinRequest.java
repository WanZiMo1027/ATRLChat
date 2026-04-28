package com.yuntian.chat_app.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatGroupJoinRequest {
    private Long id;
    private Long groupId;
    private Long userId;
    private String nickname;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
