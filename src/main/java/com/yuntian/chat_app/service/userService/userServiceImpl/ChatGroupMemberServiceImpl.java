package com.yuntian.chat_app.service.userService.userServiceImpl;

import com.yuntian.chat_app.entity.ChatGroup;
import com.yuntian.chat_app.entity.ChatGroupMember;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMemberMapper;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMapper;
import com.yuntian.chat_app.service.userService.ChatGroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatGroupMemberServiceImpl implements ChatGroupMemberService {


    private final ChatGroupMemberMapper memberMapper;

    private final ChatGroupMapper groupMapper;

    @Override
    public boolean joinGroup(Long groupId, Long userId, String nickname) {
        // 1. 检查是否已在群组中
        if (isMemberInGroup(groupId, userId)) {
            return false;
        }

        // 2. 检查群组人数
        int memberCount = memberMapper.countByGroupId(groupId);
        ChatGroup group = groupMapper.selectById(groupId);
        if (memberCount >= group.getMaxMembers()) {
            throw new RuntimeException("群组人数已满");
        }

        // 3. 加入群组
        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole("MEMBER");
        member.setNickname(nickname);

        return memberMapper.insert(member) > 0;
    }

    @Override
    public List<ChatGroupMember> getGroupMembers(Long groupId) {
        return memberMapper.selectByGroupIdWithUser(groupId);
    }

    @Override
    public boolean isMemberInGroup(Long groupId, Long userId) {
        return memberMapper.selectByGroupIdAndUserId(groupId, userId) != null;
    }

    @Override
    public boolean leaveGroup(Long groupId, Long userId) {
        return memberMapper.deleteByGroupIdAndUserId(groupId, userId) > 0;
    }

    @Override
    public boolean removeMember(Long groupId, Long userId, Long operatorId) {
        // 验证操作者权限（群主/管理员）
        ChatGroupMember operator = memberMapper.selectByGroupIdAndUserId(groupId, operatorId);
        if (operator == null || "MEMBER".equals(operator.getRole())) {
            return false;
        }

        return memberMapper.deleteByGroupIdAndUserId(groupId, userId) > 0;
    }
}
