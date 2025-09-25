package com.yuntian.chat_app.service.userService.userServiceImpl;

import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.UserFollowCharacter;
import com.yuntian.chat_app.mapper.userMapper.UserFollowCharacterMapper;
import com.yuntian.chat_app.service.userService.FollowService;
import com.yuntian.chat_app.utils.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class FollowServiceImpl implements FollowService {

    @Autowired
    private UserFollowCharacterMapper userFollowCharacterMapper;


    @Override
    public Boolean followCharacter(Long id) {
        //先检查用户是否关注该角色
        Long userId = BaseContext.getCurrentId();
        UserFollowCharacter userFollowCharacter = userFollowCharacterMapper.selectByUserIdAndCharacterId(userId, id);
        //如果不存在关系则关注
        if (userFollowCharacter == null) {
            log.info("用户已关注该角色，角色ID：{}，用户ID：{}", id, userId);
            SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(0, 0);
            Long followId = idGenerator.nextId();
            userFollowCharacter = new UserFollowCharacter();
            userFollowCharacter.setFollowId(followId);
            userFollowCharacter.setUserId(userId);
            userFollowCharacter.setCharacterId(id);
            userFollowCharacter.setCreateTime(LocalDateTime.now());
            userFollowCharacter.setUpdateTime(LocalDateTime.now());
            userFollowCharacter.setStatus(1);
            userFollowCharacterMapper.followCharacter(userFollowCharacter);
            return true;
        }

        //已存在关系
        int current = userFollowCharacter.getStatus() == null ? 0 : userFollowCharacter.getStatus();
        int next = current == 1 ? 0 : 1;
        userFollowCharacter.setStatus(next);
        userFollowCharacter.setUpdateTime(LocalDateTime.now());
        userFollowCharacterMapper.updateById(userFollowCharacter);


        return next == 1 ? Boolean.TRUE : Boolean.FALSE;
    }
}
