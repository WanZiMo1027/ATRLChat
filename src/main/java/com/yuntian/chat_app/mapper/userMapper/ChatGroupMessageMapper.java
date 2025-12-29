package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.entity.ChatGroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatGroupMessageMapper {

    // 插入消息
    int insert(ChatGroupMessage message);

    // 查询群组消息（分页）
    List<ChatGroupMessage> selectByGroupId(
            @Param("groupId") Long groupId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    // 查询消息详情
    ChatGroupMessage selectById(@Param("id") Long id);

    // 逻辑删除消息
    int deleteById(@Param("id") Long id);
}