package com.yuntian.chat_app.service.userService.userServiceImpl;

import com.yuntian.chat_app.entity.ChatGroup;
import com.yuntian.chat_app.entity.ChatGroupMember;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMapper;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMemberMapper;
import com.yuntian.chat_app.service.userService.ChatGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ChatGroupServiceImpl implements ChatGroupService {

    private final ChatGroupMapper chatGroupMapper;

    private final ChatGroupMemberMapper memberMapper;

    @Override
    @Transactional
    public Long createGroup(Long creatorId, String groupName, Long characterId, String description) {
        // 1. 创建群组
        ChatGroup group = new ChatGroup();
        group.setId(generateGroupId());
        group.setName(groupName);
        group.setCreatorId(creatorId);
        group.setCharacterId(characterId);
        group.setDescription(description);
        group.setMaxMembers(500);

        chatGroupMapper.insert(group);

        // 2. 创建者自动加入群组（角色为群主）
        ChatGroupMember owner = new ChatGroupMember();
        owner.setGroupId(group.getId());
        owner.setUserId(creatorId);
        owner.setRole("OWNER");
        memberMapper.insert(owner);

        return group.getId();
    }

    private Long generateGroupId() {
        for (int i = 0; i < 20; i++) {
            long candidate = ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
            if (chatGroupMapper.selectById(candidate) == null) {
                return candidate;
            }
        }
        throw new RuntimeException("生成群号失败，请重试");
    }

    @Override
    public ChatGroup getGroupById(Long groupId) {
        return chatGroupMapper.selectById(groupId);
    }

    @Override
    public List<ChatGroup> getGroupsByCreator(Long creatorId) {
        return chatGroupMapper.selectByCreatorId(creatorId);
    }

    @Override
    public List<ChatGroup> getGroupsByUser(Long userId) {
        return memberMapper.selectGroupsByUserId(userId);
    }

    @Override
    public boolean updateGroup(ChatGroup group) {
        return chatGroupMapper.updateById(group) > 0;
    }

    @Override
    @Transactional
    public boolean deleteGroup(Long groupId, Long operatorId) {
        // 验证权限（只有群主可以解散）
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || !group.getCreatorId().equals(operatorId)) {
            return false;
        }

        return chatGroupMapper.deleteById(groupId) > 0;
    }
}
