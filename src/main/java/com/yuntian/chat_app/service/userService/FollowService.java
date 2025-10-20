package com.yuntian.chat_app.service.userService;

import com.yuntian.chat_app.entity.UserFollowCharacter;
import com.yuntian.chat_app.vo.CharacterFollowVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FollowService {

    /**
     * 关注角色或取消关注角色
     * @param id 角色ID
     */
    Boolean followCharacter(Long id);

    /**
     * 检查用户是否关注了角色
     * @param id 角色ID
     * @param userId 用户ID
     * @return true 关注了 false 未关注
     */
    Boolean isFollowCharacter(Long id, Long userId);

    /**
     * 获取用户关注列表
     * @param userId 用户ID
     * @return 关注列表
     */
    List<CharacterFollowVo> getFollowList(Long userId);

     /**
     * 获取角色被关注数量
     * @param id 角色ID
     * @return 关注数量
     */
    Integer getFollowCount(Long id);
}
