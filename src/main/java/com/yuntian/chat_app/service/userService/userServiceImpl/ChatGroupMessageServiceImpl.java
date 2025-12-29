package com.yuntian.chat_app.service.userService.userServiceImpl;

import com.yuntian.chat_app.dto.GroupChatMessageDTO;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.entity.ChatGroupMessage;
import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.mapper.userMapper.CharacterMapper;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMessageMapper;
import com.yuntian.chat_app.mapper.userMapper.UserMapper;
import com.yuntian.chat_app.service.userService.ChatGroupMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatGroupMessageServiceImpl implements ChatGroupMessageService {

    private final ChatGroupMessageMapper messageMapper;


    private final UserMapper userMapper;


    private final CharacterMapper characterMapper;

    @Override
    @Transactional
    public Long saveMessage(GroupChatMessageDTO dto) {
        ChatGroupMessage entity = new ChatGroupMessage();
        entity.setGroupId(dto.getGroupId());
        entity.setSenderId(dto.getUserId());
        entity.setSenderType(dto.getSenderType());
        entity.setCharacterId(dto.getCharacterId());
        entity.setContent(dto.getContent());
        entity.setContentType(dto.getContentType());
        entity.setImageUrl(dto.getImageUrl());
        entity.setReplyToId(dto.getReplyToId());

        messageMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public List<GroupChatMessageDTO> getGroupMessages(Long groupId, int page, int size) {
        int offset = (page - 1) * size;
        List<ChatGroupMessage> entities = messageMapper.selectByGroupId(groupId, offset, size);

        // Entity -> DTO 转换（补充发送者信息）
        return entities.stream().map(entity -> {
            GroupChatMessageDTO dto = GroupChatMessageDTO.builder()
                    .messageId(entity.getId())
                    .groupId(entity.getGroupId())
                    .userId(entity.getSenderId())
                    .content(entity.getContent())
                    .contentType(entity.getContentType())
                    .imageUrl(entity.getImageUrl())
                    .senderType(entity.getSenderType())
                    .build();

            // 补充发送者名称和头像
            if ("USER".equals(entity.getSenderType())) {
                User user = userMapper.selectById(entity.getSenderId());
                if (user != null) {
                    dto.setSenderName(user.getUsername());
                }
            } else if ("AI".equals(entity.getSenderType())) {
                Character character = characterMapper.selectById(entity.getCharacterId());
                if (character != null) {
                    dto.setSenderName(character.getName());
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean deleteMessage(Long messageId, Long operatorId) {
        // TODO可以增加权限验证
        return messageMapper.deleteById(messageId) > 0;
    }
}
