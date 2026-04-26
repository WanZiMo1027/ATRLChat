package com.yuntian.chat_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "后台用户状态更新请求")
public class AdminUserStatusUpdateDTO implements Serializable {
    @Schema(description = "用户状态，0 表示正常，1 表示封禁", example = "1", allowableValues = {"0", "1"})
    private Integer status;
}
