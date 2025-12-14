package com.yuntian.chat_app.service.adminService.adminServiceImpl;

import com.yuntian.chat_app.entity.Admin;
import com.yuntian.chat_app.exception.UserException;
import com.yuntian.chat_app.mapper.adminMapper.AdminMapper;
import com.yuntian.chat_app.service.adminService.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;

    /**
     * 管理员登录
     * @param adminLoginReq
     * @return
     */
    @Override
    public Admin login(Admin adminLoginReq) {
        String adminName = adminLoginReq.getAdminName();
        String password = adminLoginReq.getPassword();

        // 1. 根据用户名查询数据库
        Admin admin = adminMapper.selectByAdminName(adminName);

        // 2. 判断用户是否存在
        if (admin == null) {
            throw new UserException(UserException.USER_NOT_FOUND, "账号不存在");
        }

        // 3. 校验密码
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!encryptedPassword.equals(admin.getPassword())) {
            throw new UserException(UserException.PASSWORD_ERROR, "密码错误");
        }

        // 4. 判断账号是否被禁用/删除
        if (admin.getIsDeleted() != null && admin.getIsDeleted() == 1) {
            throw new UserException(UserException.USER_NOT_FOUND, "账号已被禁用");
        }

        return admin;
    }

    /**
     * 管理员注册
     * @param admin
     * @return
     */
    @Override
    public Integer register(Admin admin) {
        // 1. 检查用户名是否已存在
        Admin existAdmin = adminMapper.selectByAdminName(admin.getAdminName());
        if (existAdmin != null) {
            throw new UserException(UserException.USERNAME_EXISTS, "用户名已存在");
        }

        // 2. 密码加密
        String password = admin.getPassword();
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        admin.setPassword(encryptedPassword);

        // 3. 设置默认值
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        admin.setIsDeleted(0);

        // 4. 插入数据库
        adminMapper.insert(admin);
        
        return 1;
    }
}
