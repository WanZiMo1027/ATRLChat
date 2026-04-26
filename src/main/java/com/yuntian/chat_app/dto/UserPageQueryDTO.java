package com.yuntian.chat_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "后台用户分页查询条件")
public class UserPageQueryDTO implements Serializable {
    @Schema(description = "页码，从 1 开始", example = "1")
    private Integer page;
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize;
    @Schema(description = "用户名关键字", example = "alice")
    private String username;
}
