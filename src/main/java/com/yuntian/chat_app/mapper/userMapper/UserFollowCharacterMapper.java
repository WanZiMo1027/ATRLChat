package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.entity.UserFollowCharacter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
    @Select("select * from user_follow_character where user_id=#{userId} and status=1")
    List<UserFollowCharacter> selectFollowList(Long userId);
}
