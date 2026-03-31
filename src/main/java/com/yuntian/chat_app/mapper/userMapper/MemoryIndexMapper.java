package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.entity.MemoryIndexDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemoryIndexMapper {

    MemoryIndexDO findByMemoryIdAndKey(@Param("memoryId") String memoryId,
                                       @Param("memoryKey") String memoryKey);

    int upsert(@Param("memoryId") String memoryId,
               @Param("memoryKey") String memoryKey,
               @Param("embeddingId") String embeddingId);
}
