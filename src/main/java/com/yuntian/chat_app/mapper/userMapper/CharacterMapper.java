package com.yuntian.chat_app.mapper.userMapper;


import com.yuntian.chat_app.entity.Character;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CharacterMapper {

    int insert(Character character);

    /**
     * 更新角色头像
     * @param characterId 角色ID
     * @param imageUrl 头像URL
     * @return 更新结果
     */
    int updateImage(@Param("characterId") Long characterId,
                    @Param("imageUrl") String imageUrl);
}
