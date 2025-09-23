package com.yuntian.chat_app.service.userService;

import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.vo.UserLoginVo;
import org.springframework.stereotype.Service;


@Service
public interface UserService {

    /**
     * 用户登录
     * @param user 用户登录
     * @return 登录成功后的用户信息
     */
    User login(User user);

    /**
     * 注册用户
     * @param user
     * @return
     */
    boolean register(User user);

    /**
     * 修改用户信息
     * @param user
     * @return
     */
    boolean update(User user);

    /**
     * 根据id查找用户
     * @param id
     * @return
     */
    User getById(Long id);

    /**
     * 更新用户头像
     * @param currentUserId
     * @param imageUrl
     */
    void updateUserAvatar(Long currentUserId, String imageUrl);
}
