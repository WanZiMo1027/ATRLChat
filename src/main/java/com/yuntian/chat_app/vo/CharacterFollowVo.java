package com.yuntian.chat_app.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterFollowVo implements Serializable {

        private Long id;
        private String name;
        private String image;
        private String appearance;
        private String background;
}
