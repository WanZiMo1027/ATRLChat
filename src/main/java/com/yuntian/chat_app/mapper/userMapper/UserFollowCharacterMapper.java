package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.entity.UserFollowCharacter;
import com.yuntian.chat_app.vo.CharacterFollowVo;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserFollowCharacterMapper {
    /**
     * 关注角色
     * @param userFollowCharacter
     */
    @Insert("insert into user_follow_character(follow_id,user_id,character_id,create_time,update_time,status) " +
            "values(#{followId},#{userId},#{characterId},#{createTime},#{updateTime},#{status})")
    void followCharacter(UserFollowCharacter userFollowCharacter);


    @Select("select * from user_follow_character where user_id=#{userId} and character_id=#{id}")
    UserFollowCharacter selectByUserIdAndCharacterId(Long userId, Long id);

    /**
     * 更新关注状态
     * @param userFollowCharacter
     */
    @Update("update user_follow_character set status=#{status},update_time=#{updateTime} where follow_id=#{followId}")
    void updateById(UserFollowCharacter userFollowCharacter);

    /**
     * 获取用户关注列表
     * @param userId 用户ID
     * @return 关注列表
     */
    @Select("select c.id, c.name ,c.image,c.appearance,c.background from user_follow_character u ,`character` c where u.character_id=c.id and u.user_id=#{userId} and status=1")
    List<CharacterFollowVo> selectFollowList(Long userId);

    /**
     * 获取角色被关注数量
     * @param id 角色ID
     * @return 关注数量
     */
    @Select("select count(*) from user_follow_character where character_id=#{id} and status=1")
    Integer selectFollowCount(Long id);

    /**
     * 查询关注某个角色的所有用户ID
     * @param characterId 角色ID
     * @return 用户ID列表
     */
    @Select("select distinct user_id from user_follow_character where character_id=#{characterId} and status=1")
    List<Long> selectFollowerUserIdsByCharacterId(Long characterId);

    /**
     * 查询关注排行榜 - 支持时间维度
     * @param startTime 开始时间（null表示不限制）
     * @param endTime 结束时间（null表示不限制）
     * @param limit 返回数量
     * @return 排行榜列表
     */
    List<CharacterFollowVo> selectFollowRank(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("limit") Integer limit);
}
