package com.yuntian.chat_app.service.userService.userServiceImpl;

import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.mapper.userMapper.UserMapper;
import com.yuntian.chat_app.service.userService.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;


@Service
public class UserServiceImpl implements UserService {


    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(User user) {

        String username = user.getUsername();
        String password = user.getPassword();
        User user1 = userMapper.selectByUsername(username);
        if(user1 == null){
            throw new RuntimeException("用户不存在");
        }
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if(!password.equals(user1.getPassword())){
            throw new RuntimeException("密码错误");
        }
        if (user1.getIsDeleted() == 1){
            throw new RuntimeException("用户已被删除");
        }


        return user1;
    }

    /**
     * 注册用户
     * @param user
     * @return
     */
    @Override
    public boolean register(User user) {
        String username = user.getUsername();
        User user1 = userMapper.selectByUsername(username);
        if(user1 != null){
            throw new RuntimeException("用户已存在");
        }
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        int insert = userMapper.insert(user);
        return insert > 0;
    }
}
