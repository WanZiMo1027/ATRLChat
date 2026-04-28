package com.yuntian.chat_app.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterTagVo implements Serializable {
    @JsonIgnore
    private Long characterId;
    private Long id;
    private String name;
    private String color;
    private Integer sortOrder;
    private Long characterCount;
}
