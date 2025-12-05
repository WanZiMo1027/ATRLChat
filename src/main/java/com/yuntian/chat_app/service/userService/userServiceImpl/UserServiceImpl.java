package com.yuntian.chat_app.service.userService.userServiceImpl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.exception.UserException;
import com.yuntian.chat_app.mapper.userMapper.UserMapper;
import com.yuntian.chat_app.service.userService.UserService;

import com.yuntian.chat_app.utils.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class UserServiceImpl implements UserService {


    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String USER_REDIS_KEY = "user:";

    @Override
    public User login(User user) {

        String username = user.getUsername();
        String password = user.getPassword();
        User user1 = userMapper.selectByUsername(username);
        if (user1 == null) {
            throw new UserException(UserException.USER_NOT_FOUND, "用户不存在");
        }
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(user1.getPassword())) {
            throw new UserException(UserException.PASSWORD_ERROR, "密码错误");
        }
        if (user1.getIsDeleted() == 1) {
            throw new UserException(UserException.USER_NOT_FOUND, "用户已被删除");
        }


        return user1;
    }

    /**
     * 注册用户
     *
     * @param user
     * @return
     */
    @Override
    public boolean register(User user) {
        String username = user.getUsername();
        User user1 = userMapper.selectByUsername(username);
        if (user1 != null) {
            throw new RuntimeException("用户已存在");
        }
        //生成随机id
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(0, 0);
        user.setId(snowflakeIdGenerator.nextId());
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        int insert = userMapper.insert(user);
        //将用户信息缓存到redis
        String userKey = USER_REDIS_KEY + user.getId();
        stringRedisTemplate.opsForValue().set(userKey, JSONUtil.toJsonStr(user), 7, TimeUnit.DAYS);
        return insert > 0;
    }

    /**
     * 修改用户信息
     *
     * @param user
     * @return
     */
    @Override
    public boolean update(User user) {

        int update = userMapper.update(user);
        return update > 0;
    }

    /**
     * 根据id查找用户
     *
     * @param id
     * @return
     */
    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 更新用户头像
     * @param currentUserId
     * @param imageUrl
     */
    @Override
    @Transactional
    public void updateUserAvatar(Long currentUserId, String imageUrl) {
        userMapper.updateAvatar(currentUserId, imageUrl);

        //更新redis缓存
        String userKey = USER_REDIS_KEY + currentUserId;
        stringRedisTemplate.opsForValue().set(userKey, JSONUtil.toJsonStr(userMapper.selectById(currentUserId)), 7, TimeUnit.DAYS);
        log.info("更新用户头像，用户ID：{}，头像URL：{}", currentUserId, imageUrl);

    }

    /**
     * 获取用户头像
     * @return
     */
    @Override
    public String getUserAvatar() {
        Long currentUserId = BaseContext.getCurrentId();
        User user = userMapper.selectById(currentUserId);
        return user.getAvatarUrl();
    }
}
