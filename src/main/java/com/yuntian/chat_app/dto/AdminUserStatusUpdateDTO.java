package com.yuntian.chat_app.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AdminUserStatusUpdateDTO implements Serializable {
    private Integer status;
}
