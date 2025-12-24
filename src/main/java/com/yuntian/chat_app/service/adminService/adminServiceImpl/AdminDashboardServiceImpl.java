package com.yuntian.chat_app.service.adminService.adminServiceImpl;

import com.yuntian.chat_app.mapper.adminMapper.AdminDashboardMapper;
import com.yuntian.chat_app.service.adminService.AdminDashboardService;
import com.yuntian.chat_app.vo.DashboardStatsVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminDashboardMapper adminDashboardMapper;

    @Override
    public DashboardStatsVo getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6); // 含今天共7天

        // 1. 获取今日概览数据
        Long todayNewUsers = adminDashboardMapper.countNewUsersByDate(today);
        Long todayChatCount = adminDashboardMapper.countChatByDate(today);
        Long todayTokenUsage = adminDashboardMapper.countTokenUsageByDate(today);

        // 2. 获取趋势数据 (数据库返回的是稀疏数据，需要补全日期)
        List<Map<String, Object>> tokenTrendRaw = adminDashboardMapper.getTokenUsageTrend(sevenDaysAgo);
        List<Map<String, Object>> userTrendRaw = adminDashboardMapper.getUserGrowthTrend(sevenDaysAgo);

        // 转换 Raw Data 为 Map<String, Long> 方便查找
        Map<String, Long> tokenMap = convertListToMap(tokenTrendRaw);
        Map<String, Long> userMap = convertListToMap(userTrendRaw);

        // 3. 构建连续的日期列表和对应的数据列表
        List<String> dateList = new ArrayList<>();
        List<Long> tokenTrendList = new ArrayList<>();
        List<Long> userTrendList = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < 7; i++) {
            LocalDate date = sevenDaysAgo.plusDays(i);
            String dateStr = date.format(formatter);
            
            dateList.add(dateStr);
            tokenTrendList.add(tokenMap.getOrDefault(dateStr, 0L));
            userTrendList.add(userMap.getOrDefault(dateStr, 0L));
        }

        return DashboardStatsVo.builder()
                .todayNewUsers(todayNewUsers)
                .todayChatCount(todayChatCount)
                .todayTokenUsage(todayTokenUsage)
                .dateList(dateList)
                .tokenTrendList(tokenTrendList)
                .userTrendList(userTrendList)
                .build();
    }

    private Map<String, Long> convertListToMap(List<Map<String, Object>> rawList) {
        Map<String, Long> map = new HashMap<>();
        if (rawList == null) return map;

        for (Map<String, Object> entry : rawList) {
            String date = (String) entry.get("date");
            // 注意：数据库 SUM/COUNT 可能返回 BigDecimal 或 Long，需安全转换
            Number count = (Number) entry.get("count");
            map.put(date, count != null ? count.longValue() : 0L);
        }
        return map;
    }
}
