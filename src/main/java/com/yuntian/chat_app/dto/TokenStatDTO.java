package com.yuntian.chat_app.dto;

import lombok.Data;
import java.util.Date;

@Data
public class TokenStatDTO {
    private String memoryId;
    private Integer inputTokens;      // 最后一次的 input
    private Integer outputTokens;     // 所有 output 之和
    private Integer totalTokens;      // input + 所有 output
    private Integer durationMs;       // 总耗时
    private Date lastCallTime;        // 最后调用时间

    /**
     * 返回空统计
     */
    public static TokenStatDTO empty() {
        TokenStatDTO stat = new TokenStatDTO();
        stat.setInputTokens(0);
        stat.setOutputTokens(0);
        stat.setTotalTokens(0);
        stat.setDurationMs(0);
        return stat;
    }
}