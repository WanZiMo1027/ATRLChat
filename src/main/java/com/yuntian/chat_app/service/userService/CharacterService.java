package com.yuntian.chat_app.service.userService;


import com.yuntian.chat_app.entity.Character;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CharacterService {


    /**
     * 新增角色
     */
    void addCharacter(Character character);

    /**
     * 更新角色头像
     * @param characterId 角色ID
     * @param imageUrl 头像URL
     */
    void updateCharacterImage(Long characterId, String imageUrl);

    /**
     * 获取角色头像
     * @param characterId 角色ID
     * @return 头像URL
     */
    void updateCharacterAvatar(Long characterId, String imageUrl);

    /**
     * 获取当前用户的角色列表
     * @return 角色列表
     */
    List<Character> getCharacterList();

    /**
     * 根据ID获取角色详情
     * @param id 角色ID
     * @return 角色对象
     */
    Character getCharacterById(Long id);


    /**
     * 获取所有角色列表（公开模型）
     * @return 角色列表
     */
    List<Character> getPublicCharacter();
}
