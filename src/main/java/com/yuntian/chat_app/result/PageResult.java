package com.yuntian.chat_app.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 封装分页查询结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "分页响应结果")
public class PageResult implements Serializable {

    private long total; //总记录数

    private List records; //当前页数据集合

}
