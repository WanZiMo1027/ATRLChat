package com.yuntian.chat_app.service.userService.userServiceImpl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.yuntian.chat_app.dto.GroupChatMessageDTO;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.entity.ChatGroupMessage;
import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.mapper.userMapper.CharacterMapper;
import com.yuntian.chat_app.mapper.userMapper.ChatGroupMessageMapper;
import com.yuntian.chat_app.mapper.userMapper.UserMapper;
import com.yuntian.chat_app.service.userService.ChatGroupMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatGroupMessageServiceImpl implements ChatGroupMessageService {

    private static final String USER_REDIS_KEY = "user:";

    private static final long USER_CACHE_TTL_DAYS = 7;

    private final ChatGroupMessageMapper messageMapper;

    private final UserMapper userMapper;

    private final CharacterMapper characterMapper;

    private final StringRedisTemplate stringRedisTemplate;

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
        Map<Long, User> userMap = loadUsers(entities);
        Map<Long, Character> characterMap = loadCharacters(entities);

        return entities.stream().map(entity -> {
            GroupChatMessageDTO dto = GroupChatMessageDTO.builder()
                    .type(2)
                    .messageId(entity.getId())
                    .groupId(entity.getGroupId())
                    .userId(entity.getSenderId())
                    .characterId(entity.getCharacterId())
                    .content(entity.getContent())
                    .contentType(entity.getContentType())
                    .imageUrl(entity.getImageUrl())
                    .replyToId(entity.getReplyToId())
                    .senderType(entity.getSenderType())
                    .timestamp(toEpochSecond(entity.getCreateTime()))
                    .build();

            if ("USER".equals(entity.getSenderType())) {
                User user = userMap.get(entity.getSenderId());
                if (user != null) {
                    dto.setSenderName(user.getUsername());
                    dto.setSenderAvatarUrl(user.getAvatarUrl());
                    dto.setAvatarUrl(user.getAvatarUrl());
                }
            } else if ("AI".equals(entity.getSenderType())) {
                Character character = characterMap.get(entity.getCharacterId());
                if (character != null) {
                    dto.setSenderName(character.getName());
                    dto.setSenderAvatarUrl(character.getImage());
                    dto.setAvatarUrl(character.getImage());
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    private Map<Long, User> loadUsers(List<ChatGroupMessage> messages) {
        Set<Long> userIds = messages.stream()
                .filter(message -> "USER".equals(message.getSenderType()))
                .map(ChatGroupMessage::getSenderId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, User> userMap = userIds.stream()
                .map(this::getCachedUser)
                .filter(user -> user != null && user.getId() != null)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));

        List<Long> missingUserIds = userIds.stream()
                .filter(userId -> !userMap.containsKey(userId))
                .collect(Collectors.toList());
        if (!missingUserIds.isEmpty()) {
            List<User> users = userMapper.selectByIds(missingUserIds);
            for (User user : users) {
                if (user != null && user.getId() != null) {
                    userMap.put(user.getId(), user);
                    cacheUser(user);
                }
            }
        }
        return userMap;
    }

    private User getCachedUser(Long userId) {
        String userJson = stringRedisTemplate.opsForValue().get(USER_REDIS_KEY + userId);
        if (StrUtil.isBlank(userJson)) {
            return null;
        }
        try {
            return JSONUtil.toBean(userJson, User.class);
        } catch (JSONException ex) {
            return null;
        }
    }

    private void cacheUser(User user) {
        stringRedisTemplate.opsForValue().set(
                USER_REDIS_KEY + user.getId(),
                JSONUtil.toJsonStr(user),
                USER_CACHE_TTL_DAYS,
                TimeUnit.DAYS
        );
    }

    private Map<Long, Character> loadCharacters(List<ChatGroupMessage> messages) {
        List<Long> characterIds = messages.stream()
                .filter(message -> "AI".equals(message.getSenderType()))
                .map(ChatGroupMessage::getCharacterId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (characterIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return characterMapper.selectByIds(characterIds).stream()
                .filter(character -> character != null && character.getId() != null)
                .collect(Collectors.toMap(Character::getId, character -> character, (left, right) -> left));
    }

    private Long toEpochSecond(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    @Override
    public boolean deleteMessage(Long messageId, Long operatorId) {
        // TODO: add permission checks when message deletion is exposed.
        return messageMapper.deleteById(messageId) > 0;
    }
}
