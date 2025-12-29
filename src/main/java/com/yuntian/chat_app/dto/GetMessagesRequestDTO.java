package com.yuntian.chat_app.dto;

import lombok.Data;

@Data
public class GetMessagesRequestDTO {
    private Long groupId;
    private Integer page = 1;     // 默认第1页
    private Integer size = 50;    // 默认50条
}
