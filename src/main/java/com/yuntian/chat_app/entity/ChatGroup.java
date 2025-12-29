package com.yuntian.chat_app.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatGroup {
    private Long id;
    private String name;
    private String avatarUrl;
    private Long creatorId;
    private Long characterId;
    private String description;
    private Integer maxMembers;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}