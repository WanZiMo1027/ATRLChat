package com.yuntian.chat_app.mapper.adminMapper;

import com.github.pagehelper.Page;
import com.yuntian.chat_app.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminUserMapper {

    /**
     * 分页查询用户列表
     * @param username 用户名（模糊查询）
     * @return
     */
    Page<User> pageQuery(@Param("username") String username);

    /**
     * 更新用户状态（封禁/解封）
     * @param id 用户ID
     * @param status 状态 (0:正常, 1:删除/封禁)
     */
    @Update("UPDATE user SET is_deleted = #{status}, update_time = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(Long id);
}
