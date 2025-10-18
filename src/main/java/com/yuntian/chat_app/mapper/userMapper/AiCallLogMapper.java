package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.dto.TokenStatDTO;
import com.yuntian.chat_app.entity.AiCallLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * 查询指定时间范围内的所有记录（不分组，在 Service 层处理）
     */
    List<Map<String, Object>> getTokenUsageByDateRange(
            @Param("userId") String userId,
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end);
}