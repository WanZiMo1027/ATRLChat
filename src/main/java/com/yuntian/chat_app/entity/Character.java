package com.yuntian.chat_app.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Character implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID，主键自增
     */
    private Long id;

    /**
     * 角色名字
     */
    private String name;

    /**
     * 角色头像URL
     */
    private String image;
    /**
     * 角色外表描述
     */
    private String appearance;

    /**
     * 角色经历背景故事
     */
    private String background;

    /**
     * 角色性格特征
     */
    private String personality;

    /**
     * 经典台词示例
     */
    private String classicLines;

    /**
     * 创建该角色的用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 是否删除，0-未删除，1-已删除
     */
    private Integer isDeleted;



}
