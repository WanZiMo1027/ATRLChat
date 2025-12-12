package com.yuntian.chat_app.mapper.userMapper;


import com.yuntian.chat_app.entity.Character;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CharacterMapper {

    int insert(Character character);

    /**
     * 更新角色头像
     *
     * @param characterId 角色ID
     * @param imageUrl    头像URL
     * @return 更新结果
     */
    int updateImage(@Param("characterId") Long characterId,
                    @Param("imageUrl") String imageUrl);

    /**
     * 根据ID更新角色信息
     *
     * @param character 角色对象
     */
    @Update("update `character` set image=#{image} where id=#{id}")
    void updateById(Character character);

    /**
     * 获取当前用户的所有角色列表（我的模型）
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Character> selectByUserId(@Param("userId") Long userId);

    /**
     * 获取所有角色列表（公开模型）
     *
     * @return 角色列表
     */
    List<Character> selectAll();

    /**
     * 根据ID查询角色详情
     *
     * @param id 角色ID
     * @return 角色对象
     */
    Character selectById(@Param("id") Long id);

    /**
     * 检索角色
     *
     * @param name        角色名称
     * @param personality 角色性格
     * @return 角色列表
     */
    List<Character> selectByKeyword(@Param("name") String name,
                                    @Param("personality") String personality);


     @Update("update `character` set is_public=#{isPublic} where id=#{id}")
    void updateIsPublic(@Param("id") Long id,
                        @Param("isPublic") Integer isPublic);

     /**
     * 根据ID更新角色信息
     * @param character 角色对象
     */
    void updateInfoById(Character character);

    /**
     * 根据ID删除角色（逻辑删除）
     *
     * @param characterId 角色ID
     */
    @Update("update `character` set is_deleted=1 where id=#{characterId}")
    void deleteById(Long characterId);
}