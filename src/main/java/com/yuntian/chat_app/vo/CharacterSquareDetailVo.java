package com.yuntian.chat_app.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterSquareDetailVo implements Serializable {
    private Long id;
    private String name;
    private String image;
    private String intro;
    private Integer followCount;
    private Boolean isFollowed;
}
