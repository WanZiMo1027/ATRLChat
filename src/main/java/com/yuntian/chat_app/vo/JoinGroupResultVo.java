package com.yuntian.chat_app.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinGroupResultVo {
    private String status;
    private Long groupId;
    private Long requestId;
    private String message;
}
