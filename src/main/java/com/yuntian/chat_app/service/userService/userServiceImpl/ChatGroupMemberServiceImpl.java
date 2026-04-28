package com.yuntian.chat_app.service.userService.userServiceImpl;

import com.yuntian.chat_app.entity.ChatGroup;
import com.yuntian.chat_app.entity.ChatGroupJoinRequest;
import com.yuntian.chat_app.entity.ChatGroupMember;
import com.yuntian.chat_app.exception.GroupException;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupJoinRequestMapper;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMapper;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMemberMapper;
import com.yuntian.chat_app.service.userService.ChatGroupMemberService;
import com.yuntian.chat_app.vo.ChatGroupJoinRequestVo;
import com.yuntian.chat_app.vo.JoinGroupResultVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatGroupMemberServiceImpl implements ChatGroupMemberService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final Set<String> VALID_REQUEST_STATUSES =
            Set.of(STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED);

    private final ChatGroupMemberMapper memberMapper;

    private final ChatGroupMapper groupMapper;

    private final ChatGroupJoinRequestMapper joinRequestMapper;

    @Override
    @Transactional
    public JoinGroupResultVo joinGroup(Long groupId, Long userId, String nickname) {
        if (userId == null) {
            throw new GroupException(GroupException.GROUP_PERMISSION_DENIED, "未登录");
        }

        ChatGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new GroupException(GroupException.GROUP_NOT_FOUND, "群不存在");
        }

        if (isMemberInGroup(groupId, userId)) {
            return new JoinGroupResultVo("ALREADY_MEMBER", groupId, null, "已在群聊中");
        }

        ensureGroupHasCapacity(group);

        if (isJoinApprovalEnabled(group)) {
            ChatGroupJoinRequest pending = joinRequestMapper.selectPendingByGroupIdAndUserId(groupId, userId);
            if (pending != null) {
                return new JoinGroupResultVo("PENDING", groupId, pending.getId(), "入群申请待审核");
            }

            ChatGroupJoinRequest request = new ChatGroupJoinRequest();
            request.setGroupId(groupId);
            request.setUserId(userId);
            request.setNickname(nickname);
            request.setStatus(STATUS_PENDING);
            joinRequestMapper.insert(request);
            return new JoinGroupResultVo("PENDING", groupId, request.getId(), "入群申请已提交，等待管理员审核");
        }

        insertMember(groupId, userId, nickname, "MEMBER");
        return new JoinGroupResultVo("JOINED", groupId, null, "已加入群聊");
    }

    @Override
    public List<ChatGroupMember> getGroupMembers(Long groupId) {
        return memberMapper.selectByGroupIdWithUser(groupId);
    }

    @Override
    public int countGroupMembers(Long groupId) {
        return memberMapper.countByGroupId(groupId);
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
        ChatGroupMember operator = memberMapper.selectByGroupIdAndUserId(groupId, operatorId);
        if (operator == null || "MEMBER".equals(operator.getRole())) {
            return false;
        }

        return memberMapper.deleteByGroupIdAndUserId(groupId, userId) > 0;
    }

    @Override
    public List<ChatGroupJoinRequestVo> getJoinRequests(Long groupId, String status, Long operatorId) {
        ensureGroupAdmin(groupId, operatorId);
        String normalizedStatus = normalizeStatus(status);
        return joinRequestMapper.selectByGroupIdAndStatus(groupId, normalizedStatus);
    }

    @Override
    @Transactional
    public boolean approveJoinRequest(Long requestId, Long operatorId) {
        ChatGroupJoinRequest request = getRequestOrThrow(requestId);
        ensureGroupAdmin(request.getGroupId(), operatorId);
        if (!STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }

        if (!isMemberInGroup(request.getGroupId(), request.getUserId())) {
            ChatGroup group = getGroupOrThrow(request.getGroupId());
            ensureGroupHasCapacity(group);
            int updated = joinRequestMapper.updateStatus(requestId, STATUS_APPROVED, operatorId, null);
            if (updated <= 0) {
                return false;
            }
            insertMember(request.getGroupId(), request.getUserId(), request.getNickname(), "MEMBER");
            return true;
        }

        return joinRequestMapper.updateStatus(requestId, STATUS_APPROVED, operatorId, null) > 0;
    }

    @Override
    public boolean rejectJoinRequest(Long requestId, Long operatorId, String reason) {
        ChatGroupJoinRequest request = getRequestOrThrow(requestId);
        ensureGroupAdmin(request.getGroupId(), operatorId);
        if (!STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        String safeReason = reason == null || reason.isBlank() ? null : reason.trim();
        return joinRequestMapper.updateStatus(requestId, STATUS_REJECTED, operatorId, safeReason) > 0;
    }

    private void insertMember(Long groupId, Long userId, String nickname, String role) {
        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setNickname(nickname);
        memberMapper.insert(member);
    }

    private boolean isJoinApprovalEnabled(ChatGroup group) {
        return group.getJoinRequiresApproval() != null && group.getJoinRequiresApproval() == 1;
    }

    private void ensureGroupHasCapacity(ChatGroup group) {
        int memberCount = memberMapper.countByGroupId(group.getId());
        if (group.getMaxMembers() != null && memberCount >= group.getMaxMembers()) {
            throw new GroupException(GroupException.GROUP_FULL, "群组人数已满");
        }
    }

    private ChatGroupJoinRequest getRequestOrThrow(Long requestId) {
        ChatGroupJoinRequest request = joinRequestMapper.selectById(requestId);
        if (request == null) {
            throw new GroupException(GroupException.GROUP_JOIN_REQUEST_NOT_FOUND, "入群申请不存在");
        }
        return request;
    }

    private ChatGroup getGroupOrThrow(Long groupId) {
        ChatGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new GroupException(GroupException.GROUP_NOT_FOUND, "群不存在");
        }
        return group;
    }

    private void ensureGroupAdmin(Long groupId, Long operatorId) {
        if (operatorId == null) {
            throw new GroupException(GroupException.GROUP_PERMISSION_DENIED, "未登录");
        }
        getGroupOrThrow(groupId);
        ChatGroupMember operator = memberMapper.selectByGroupIdAndUserId(groupId, operatorId);
        if (operator == null || "MEMBER".equals(operator.getRole())) {
            throw new GroupException(GroupException.GROUP_PERMISSION_DENIED, "无权限操作入群申请");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_PENDING;
        }
        String normalized = status.trim().toUpperCase();
        if (!VALID_REQUEST_STATUSES.contains(normalized)) {
            throw new GroupException(GroupException.GROUP_INVALID_JOIN_REQUEST_STATUS, "入群申请状态不正确");
        }
        return normalized;
    }
}
