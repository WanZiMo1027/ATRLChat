package com.yuntian.chat_app.mapper.userMapper;


import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.vo.UserLoginVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
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
}
