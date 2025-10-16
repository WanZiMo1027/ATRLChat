package com.yuntian.chat_app.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiCallLogDO {
    private Long id;
    private String userId;
    private String characterId;
    private String modelName;
    private String memoryId;
    private String status;        // success / error
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Integer durationMs;
    private Date requestTs;
    private Date createdAt;
}
