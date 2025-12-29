package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.entity.ChatGroup;
import com.yuntian.chat_app.entity.ChatGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatGroupMemberMapper {

    // 添加成员
    int insert(ChatGroupMember member);

    // 查询群组成员列表
    List<ChatGroupMember> selectByGroupId(@Param("groupId") Long groupId);

    // 查询用户加入的群组
    List<ChatGroupMember> selectByUserId(@Param("userId") Long userId);

    // 查询用户加入的群组详情
    List<ChatGroup> selectGroupsByUserId(@Param("userId") Long userId);

    // 查询特定成员
    ChatGroupMember selectByGroupIdAndUserId(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId
    );

    // 统计群组成员数量
    int countByGroupId(@Param("groupId") Long groupId);

    // 移除成员（逻辑删除）
    int deleteByGroupIdAndUserId(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId
    );
}
