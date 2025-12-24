package com.yuntian.chat_app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsVo implements Serializable {

    // 今日数据概览
    private Long todayNewUsers;       // 今日新增用户
    private Long todayChatCount;      // 今日对话次数
    private Long todayTokenUsage;     // 今日消耗Token

    // 趋势图数据 (最近7天)
    private List<String> dateList;           // 日期列表 [12-01, 12-02...]
    private List<Long> tokenTrendList;       // Token消耗趋势
    private List<Long> userTrendList;        // 用户增长趋势
}
