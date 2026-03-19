package com.yuntian.chat_app.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserPageQueryDTO implements Serializable {
    private Integer page;
    private Integer pageSize;
    private String username;
}
