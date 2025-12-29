package com.yuntian.chat_app.dto;



import lombok.Data;

@Data
public class JoinGroupRequestDTO {
    private Long groupId;
    private Long userId;
    private String nickname;
}
