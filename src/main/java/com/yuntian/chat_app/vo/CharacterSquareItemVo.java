package com.yuntian.chat_app.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterSquareItemVo implements Serializable {
    private Long id;
    private String name;
    private String image;
    private String intro;
    private List<CharacterTagVo> tags = new ArrayList<>();
    private Integer followCount;
    private Boolean isFollowed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
