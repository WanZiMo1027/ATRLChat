package com.yuntian.chat_app.service.userService.userServiceImpl;

import com.yuntian.chat_app.entity.ChatGroup;
import com.yuntian.chat_app.entity.ChatGroupMember;
import com.yuntian.chat_app.exception.GroupException;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMapper;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMemberMapper;
import com.yuntian.chat_app.service.userService.ChatGroupService;
import com.yuntian.chat_app.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ChatGroupServiceImpl implements ChatGroupService {

    private final ChatGroupMapper chatGroupMapper;

    private final ChatGroupMemberMapper memberMapper;

    private final AliOssUtil aliOssUtil;

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
    public String updateGroupAvatar(Long groupId, Long operatorId, MultipartFile file) {
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new GroupException(GroupException.GROUP_NOT_FOUND, "群不存在");
        }
        if (!group.getCreatorId().equals(operatorId)) {
            throw new GroupException(GroupException.GROUP_PERMISSION_DENIED, "无权限修改群头像");
        }
        validateAvatarFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        String objectName = String.format("group_avatars/%d_%d%s",
                groupId, System.currentTimeMillis(), extension);

        try {
            String imageUrl = aliOssUtil.upload(file.getBytes(), objectName);
            ChatGroup patch = new ChatGroup();
            patch.setId(groupId);
            patch.setAvatarUrl(imageUrl);
            if (!updateGroup(patch)) {
                throw new GroupException(GroupException.GROUP_AVATAR_UPLOAD_FAILED, "群头像保存失败");
            }
            return imageUrl;
        } catch (IOException e) {
            throw new GroupException(GroupException.GROUP_AVATAR_UPLOAD_FAILED, "群头像上传失败");
        }
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

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new GroupException(GroupException.GROUP_AVATAR_INVALID_TYPE, "请上传头像图片");
        }

        String originalFilename = file.getOriginalFilename();
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex < 0) {
            throw new GroupException(GroupException.GROUP_AVATAR_INVALID_TYPE, "仅支持 jpg、jpeg、png、webp 格式图片");
        }

        String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (!".jpg".equals(extension) && !".jpeg".equals(extension)
                && !".png".equals(extension) && !".webp".equals(extension)) {
            throw new GroupException(GroupException.GROUP_AVATAR_INVALID_TYPE, "仅支持 jpg、jpeg、png、webp 格式图片");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            throw new GroupException(GroupException.GROUP_AVATAR_TOO_LARGE, "图片大小不能超过2MB");
        }
    }
}
