package com.yuntian.chat_app.mapper.adminMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminDashboardMapper {

    /**
     * 统计指定日期的注册用户数
     */
    @Select("SELECT COUNT(*) FROM app_user WHERE DATE(create_time) = #{date}")
    Long countNewUsersByDate(@Param("date") LocalDate date);

    /**
     * 统计指定日期的对话次数 (AI调用次数)
     */
    @Select("SELECT COUNT(*) FROM ai_call_log WHERE DATE(request_ts) = #{date} AND status = 'success'")
    Long countChatByDate(@Param("date") LocalDate date);

    /**
     * 统计指定日期的Token消耗总量
     */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM ai_call_log WHERE DATE(request_ts) = #{date} AND status = 'success'")
    Long countTokenUsageByDate(@Param("date") LocalDate date);

    /**
     * 获取过去N天的每日Token消耗 (返回: date -> count)
     */
    @Select("SELECT TO_CHAR(request_ts, 'YYYY-MM-DD') as date, SUM(total_tokens) as count " +
            "FROM ai_call_log " +
            "WHERE request_ts >= #{startDate} AND status = 'success' " +
            "GROUP BY TO_CHAR(request_ts, 'YYYY-MM-DD') " +
            "ORDER BY date ASC")
    List<Map<String, Object>> getTokenUsageTrend(@Param("startDate") LocalDate startDate);

    /**
     * 获取过去N天的每日新增用户 (返回: date -> count)
     */
    @Select("SELECT TO_CHAR(create_time, 'YYYY-MM-DD') as date, COUNT(*) as count " +
            "FROM app_user " +
            "WHERE create_time >= #{startDate} " +
            "GROUP BY TO_CHAR(create_time, 'YYYY-MM-DD') " +
            "ORDER BY date ASC")
    List<Map<String, Object>> getUserGrowthTrend(@Param("startDate") LocalDate startDate);
}
