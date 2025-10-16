package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.dto.TokenStatDTO;
import com.yuntian.chat_app.entity.AiCallLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiCallLogMapper {
    /**
     * 插入记录
     */
    int insert(AiCallLogDO row);

    /**
     * 查询指定 memoryId 的所有记录（明细）
     */
    List<AiCallLogDO> findAllByMemoryId(@Param("memoryId") String memoryId);

    /**
     * 查询指定 memoryId 的最后一条记录
     */
    AiCallLogDO findLatestByMemoryId(@Param("memoryId") String memoryId);
}