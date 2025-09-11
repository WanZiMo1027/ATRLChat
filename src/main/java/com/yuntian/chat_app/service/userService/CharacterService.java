package com.yuntian.chat_app.service.userService;


import com.yuntian.chat_app.entity.Character;
import org.springframework.stereotype.Service;

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
}
