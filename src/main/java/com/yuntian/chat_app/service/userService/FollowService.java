package com.yuntian.chat_app.service.userService;

import org.springframework.stereotype.Service;

@Service
public interface FollowService {

    /**
     * 关注角色
     * @param id 角色ID
     */
    Boolean followCharacter(Long id);
}
