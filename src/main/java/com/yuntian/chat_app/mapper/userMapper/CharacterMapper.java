package com.yuntian.chat_app.mapper.userMapper;


import com.yuntian.chat_app.entity.Character;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 根据ID更新角色信息
     * @param character 角色对象
     */
    @Update("update `character` set image=#{image} where id=#{id}")
    void updateById(Character character);
}
