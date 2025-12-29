package com.yuntian.chat_app.service.userService;

import com.yuntian.chat_app.dto.GroupChatMessageDTO;

import java.util.List;

public interface ChatGroupMessageService {

    /**
     * 保存消息
     */
    Long saveMessage(GroupChatMessageDTO dto);

    /**
     * 查询群历史消息
     */
    List<GroupChatMessageDTO> getGroupMessages(Long groupId, int page, int size);

    /**
     * 删除消息
     */
    boolean deleteMessage(Long messageId, Long operatorId);
}