package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.entity.PrivateChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrivateChatMessageMapper {

    // 插入消息
    int insert(PrivateChatMessage message);

    // 查询历史记录 (分页)
    // 按照 memoryId 查，时间正序（旧消息在前，新消息在后，符合聊天习惯）
    List<PrivateChatMessage> selectByMemoryId(
            @Param("memoryId") String memoryId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    // 逻辑删除（可选）
    int deleteById(@Param("id") Long id);
}
