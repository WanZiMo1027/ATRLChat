package com.yuntian.chat_app.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatGroupMember {
    private Long id;
    private Long groupId;
    private Long userId;
    private String role;  // OWNER, ADMIN, MEMBER
    private String nickname;
    private String username;
    private String avatarUrl;
    private LocalDateTime joinTime;
    private LocalDateTime lastReadTime;
    private Integer isMuted;
    private Integer isDeleted;
}
