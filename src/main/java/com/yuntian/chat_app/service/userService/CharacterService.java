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

     /**
     * 检索角色
     * @param name 角色名称
     * @param personality 角色性格
     * @return 角色列表
     */
    List<Character> searchCharacter(String name, String personality);

     /**
     * 公开角色
     * @param characterId 角色ID
     * 0-不公开，1-公开
     * @return 操作结果，0-失败，1-成功
     */
    Integer publicOrNotCharacter(Long characterId);


    /**
     * 修改角色信息
     * @param character 角色对象
     */
    void updateCharacter(Character character);
    /**
     * 删除角色
     * @param characterId 角色ID
     */
    void deleteCharacter(Long characterId);
}
