package com.yuntian.chat_app.mapper.adminMapper;

import com.yuntian.chat_app.entity.Admin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminMapper {

    /**
     * 根据用户名查询管理员
     * @param adminName
     * @return
     */
    @Select("select * from admin where admin_name = #{adminName}")
    Admin selectByAdminName(String adminName);

    /**
     * 插入管理员
     * @param admin
     */
    @Insert("insert into admin(admin_name, password, email, phone, create_time, update_time, is_deleted) " +
            "values(#{adminName}, #{password}, #{email}, #{phone}, #{createTime}, #{updateTime}, #{isDeleted})")
    void insert(Admin admin);
}
