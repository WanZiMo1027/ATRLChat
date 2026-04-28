package com.yuntian.chat_app.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatGroupJoinRequestVo {
    private Long requestId;
    private Long groupId;
    private Long userId;
    private String nickname;
    private String username;
    private String avatarUrl;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
