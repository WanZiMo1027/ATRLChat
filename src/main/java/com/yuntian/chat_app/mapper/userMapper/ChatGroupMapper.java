package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.entity.ChatGroup;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatGroupMapper {
    // 插入群组
    int insert(ChatGroup group);

    // 根据ID查询
    ChatGroup selectById(@Param("id") Long id);

    // 根据创建者查询群组列表
    List<ChatGroup> selectByCreatorId(@Param("creatorId") Long creatorId);

    // 更新群组信息
    int updateById(ChatGroup group);

    // 逻辑删除
    int deleteById(@Param("id") Long id);
}
