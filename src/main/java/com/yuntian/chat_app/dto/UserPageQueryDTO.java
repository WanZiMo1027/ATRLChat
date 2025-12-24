package com.yuntian.chat_app.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserPageQueryDTO implements Serializable {
    private int page;
    private int pageSize;
    private String username;
}
