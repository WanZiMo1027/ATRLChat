package com.yuntian.chat_app.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorContext implements Serializable {
    private String userId;
    private String characterId;
    private String memoryId;     // 添加会话 ID

    @Serial
    private static final long serialVersionUID = 1L;
}
