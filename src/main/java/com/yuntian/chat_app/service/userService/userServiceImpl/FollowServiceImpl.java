package com.yuntian.chat_app.service.userService.userServiceImpl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.UserFollowCharacter;
import com.yuntian.chat_app.mapper.userMapper.UserFollowCharacterMapper;
import com.yuntian.chat_app.service.userService.FollowService;
import com.yuntian.chat_app.utils.SnowflakeIdGenerator;
import com.yuntian.chat_app.vo.CharacterFollowVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;



@Service
@Slf4j
public class FollowServiceImpl implements FollowService {

    @Autowired
    private UserFollowCharacterMapper userFollowCharacterMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String FOLLOW_KEY = "follow:";
    private static final int CACHE_TTL_DAYS = 1;

    /**
     * 关注角色或取消关注角色
     * @param id 角色ID
     * @return 是否关注成功
     */
    @Override
    public Boolean followCharacter(Long id) {
        //先检查用户是否关注该角色
        Long userId = BaseContext.getCurrentId();
        String followKey = FOLLOW_KEY + userId + ":" + id;
        String followJson = stringRedisTemplate.opsForValue().get(followKey);
        //如果在缓存中查到关系，直接更新状态
        if(StrUtil.isNotBlank(followJson)){
            //将followJson转换为UserFollowCharacter对象
            UserFollowCharacter userFollowCharacter = JSONUtil.toBean(followJson, UserFollowCharacter.class);
            //更新状态
            return updateStatus(userFollowCharacter, followKey);
        }else {
            //如果未查到则从数据库查询
            UserFollowCharacter rel = userFollowCharacterMapper.selectByUserIdAndCharacterId(userId, id);
            //如果查不到则创建新关系
            if(rel == null){
                log.info("用户未关注该角色，角色ID：{}，用户ID：{}", id, userId);
                SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(0, 0);
                Long followId = idGenerator.nextId();
                rel = new UserFollowCharacter();
                rel.setFollowId(followId);
                rel.setUserId(userId);
                rel.setCharacterId(id);
                rel.setCreateTime(LocalDateTime.now());
                rel.setUpdateTime(LocalDateTime.now());
                rel.setStatus(1);
                userFollowCharacterMapper.followCharacter(rel);
                String newFollowJson = JSONUtil.toJsonStr(rel);
                stringRedisTemplate.opsForValue().set(followKey, newFollowJson, CACHE_TTL_DAYS, TimeUnit.DAYS);
                return rel.getStatus() == 1;
            }
            //如果查得到则更新状态
            //更新状态
                return updateStatus(rel, followKey);

        }
    }


    /**
     * 查看关注状态
     * @param id 角色ID
     * @param userId 用户ID
     * @return 关注状态
     */
    @Override
    public Boolean isFollowCharacter(Long id, Long userId) {
        UserFollowCharacter userFollowCharacter = userFollowCharacterMapper.selectByUserIdAndCharacterId(userId, id);
        return userFollowCharacter != null && userFollowCharacter.getStatus() == 1;
    }

    /**
     * 获取用户关注列表
     * @param userId 用户ID
     * @return 关注列表
     */
    @Override
    public List<CharacterFollowVo> getFollowList(Long userId) {
        return userFollowCharacterMapper.selectFollowList(userId);
    }

    //更新状态
    private Boolean updateStatus(UserFollowCharacter userFollowCharacter, String followKey){
        int current = userFollowCharacter.getStatus() == null ? 0 : userFollowCharacter.getStatus();
        int next = current == 1 ? 0 : 1;
        userFollowCharacter.setStatus(next);
        userFollowCharacter.setUpdateTime(LocalDateTime.now());
        userFollowCharacterMapper.updateById(userFollowCharacter);
        //更新缓存
        String newFollowJson = JSONUtil.toJsonStr(userFollowCharacter);
        stringRedisTemplate.opsForValue().set(followKey, newFollowJson, CACHE_TTL_DAYS, TimeUnit.DAYS);
        return next == 1;
    }
}
