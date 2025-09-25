package com.yuntian.chat_app.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFollowCharacter {
    private Long followId;
    private Long userId;
    private Long characterId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer status;
}
