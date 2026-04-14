package com.yuntian.chat_app.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDTO {

    private String username;
    private String password;
    private String email;
    private String phone;
    private String address;
}
