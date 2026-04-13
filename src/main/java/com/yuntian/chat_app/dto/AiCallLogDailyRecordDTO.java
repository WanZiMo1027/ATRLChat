package com.yuntian.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCallLogDailyRecordDTO {

    private LocalDateTime requestTs;
    private String memoryId;
    private Long totalTokens;
}
