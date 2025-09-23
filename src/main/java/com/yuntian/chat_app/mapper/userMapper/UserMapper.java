package com.yuntian.chat_app.mapper.userMapper;


import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.vo.UserLoginVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Service;

@Mapper
public interface UserMapper {

    @Select("select * from user where username = #{username}")
    User selectByUsername(String username);

    /**
     * 注册用户
     * @param user
     * @return
     */

    int insert(User user);

    /**
     * 根据id查找用户
     * @param id
     * @return
     */
    @Select("select * from user where id = #{id}")
    User selectById(Long id);

    int update(User user);


    @Update("update user set avatar_url = #{imageUrl} where id = #{currentUserId}")
    void updateAvatar(Long currentUserId, String imageUrl);
}
