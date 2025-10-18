package com.yuntian.chat_app.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyTokenDTO {

    private String date;
    private Long totalTokens;
}
