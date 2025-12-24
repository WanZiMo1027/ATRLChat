package com.yuntian.chat_app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVo implements Serializable {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private LocalDateTime createTime;
    private Integer isDeleted; // 0: 正常, 1: 已删除/禁用
}
