package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.entity.PrivateChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrivateChatMessageMapper {

    int insert(PrivateChatMessage message);

    List<PrivateChatMessage> selectByMemoryId(
            @Param("memoryId") String memoryId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    PrivateChatMessage selectLatestByMemoryId(@Param("memoryId") String memoryId);

    int deleteById(@Param("id") Long id);
}
