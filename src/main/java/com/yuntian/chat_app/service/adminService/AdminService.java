package com.yuntian.chat_app.service.adminService;

import com.yuntian.chat_app.entity.Admin;
import com.yuntian.chat_app.entity.User;
import org.springframework.stereotype.Service;


@Service
public interface AdminService {

    /**
     * 用户登录
     * @param admin 用户登录
     * @return 登录成功后的用户信息
     */
    Admin login(Admin admin);

     /**
     * 管理员注册
     * @param admin 管理员注册
     * @return 注册成功后的管理员信息
     */
    Integer register(Admin admin);
}
