package com.yuntian.chat_app.service.userService;

import com.yuntian.chat_app.entity.ChatGroup;

import java.util.List;

public interface ChatGroupService {

    /**
     * 创建群组
     */
    Long createGroup(Long creatorId, String groupName, Long characterId, String description);

    /**
     * 查询群组信息
     */
    ChatGroup getGroupById(Long groupId);

    /**
     * 查询用户创建的群组
     */
    List<ChatGroup> getGroupsByCreator(Long creatorId);

    /**
     * 查询用户加入的群组
     */
    List<ChatGroup> getGroupsByUser(Long userId);

    /**
     * 更新群组信息
     */
    boolean updateGroup(ChatGroup group);

    /**
     * 解散群组
     */
    boolean deleteGroup(Long groupId, Long operatorId);
}
