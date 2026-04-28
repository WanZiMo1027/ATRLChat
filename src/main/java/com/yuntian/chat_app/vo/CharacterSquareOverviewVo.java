package com.yuntian.chat_app.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterSquareOverviewVo implements Serializable {
    private Long publicCharacterTotal;
    private Long todayNewCount;
    private List<CharacterTagVo> hotTags = new ArrayList<>();
    private List<CharacterSquareItemVo> recentActive = new ArrayList<>();
    private List<CharacterSquareItemVo> featuredCharacters = new ArrayList<>();
}
