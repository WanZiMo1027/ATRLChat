package com.yuntian.chat_app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginVo implements Serializable {

    private Long id;
    private String adminName;
    private String token;
}
