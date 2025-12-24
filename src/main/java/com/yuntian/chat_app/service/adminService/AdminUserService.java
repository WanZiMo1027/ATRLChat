package com.yuntian.chat_app.service.adminService;

import com.yuntian.chat_app.dto.UserPageQueryDTO;
import com.yuntian.chat_app.result.PageResult;

public interface AdminUserService {

    /**
     * 分页查询用户
     * @param userPageQueryDTO
     * @return
     */
    PageResult pageQuery(UserPageQueryDTO userPageQueryDTO);

    /**
     * 封禁/解封用户
     * @param id
     * @param status
     */
    void updateStatus(Long id, Integer status);
}
