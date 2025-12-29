package com.yuntian.chat_app.dto;

import lombok.Data;

@Data
public class LeaveGroupRequestDTO {
    private Long groupId;
    private Long userId;
}
