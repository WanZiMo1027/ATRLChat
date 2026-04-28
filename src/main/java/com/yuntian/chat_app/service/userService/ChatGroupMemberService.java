package com.yuntian.chat_app.service.userService;

import com.yuntian.chat_app.entity.ChatGroupMember;
import com.yuntian.chat_app.vo.ChatGroupJoinRequestVo;
import com.yuntian.chat_app.vo.JoinGroupResultVo;

import java.util.List;

public interface ChatGroupMemberService {

    /**
     * 加入群组
     */
    JoinGroupResultVo joinGroup(Long groupId, Long userId, String nickname);

    /**
     * 查询群成员列表
     */
    List<ChatGroupMember> getGroupMembers(Long groupId);

    /**
     * 统计群成员数量
     */
    int countGroupMembers(Long groupId);

    /**
     * 查询用户是否在群组中
     */
    boolean isMemberInGroup(Long groupId, Long userId);

    /**
     * 退出群组
     */
    boolean leaveGroup(Long groupId, Long userId);

    /**
     * 踢出成员
     */
    boolean removeMember(Long groupId, Long userId, Long operatorId);

    List<ChatGroupJoinRequestVo> getJoinRequests(Long groupId, String status, Long operatorId);

    boolean approveJoinRequest(Long requestId, Long operatorId);

    boolean rejectJoinRequest(Long requestId, Long operatorId, String reason);
}
