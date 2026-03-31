package com.yuntian.chat_app.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryIndexDO {
    private String memoryId;
    private String memoryKey;
    private String embeddingId;
    private Date updatedAt;
}
