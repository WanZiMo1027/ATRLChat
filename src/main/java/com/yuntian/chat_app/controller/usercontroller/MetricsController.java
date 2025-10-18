package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.dto.DailyTokenDTO;
import com.yuntian.chat_app.dto.TokenStatDTO;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.AiCallLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
@Slf4j
public class MetricsController {


    @Autowired
    private AiCallLogService aiCallLogService;
    /**
     * 查询指定会话的 Token 统计
     * GET /api/metrics/tokens/{memoryId}
     */
    @GetMapping("/tokens/{memoryId}")
    public Result<Map<String, Object>> getTokenStats(@PathVariable String memoryId) {
        log.info("查询 Token 统计 - memoryId: {}", memoryId);

        TokenStatDTO stat = aiCallLogService.getTokenStatByMemoryId(memoryId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", stat);

        return Result.success(result);
    }

    /**
     * 查询用户每天的 Token 使用量
     * GET /api/metrics/tokens/daily?begin=2025-10-08&end=2025-10-14
     *
     * @param begin 开始日期（可选，默认7天前）
     * @param end 结束日期（可选，默认今天）
     * @return 每天的统计数据
     */
    @GetMapping("/tokens/daily")
    public Result<List<DailyTokenDTO>> getDailyTokenUsage(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {

        // 从上下文获取当前用户ID
        Long userId = BaseContext.getCurrentId();

        // 默认查询最近7天
        if (end == null) {
            end = LocalDate.now();
        }
        if (begin == null) {
            begin = end.minusDays(6);  // 包含今天共7天
        }

        log.info("查询用户每日 Token 使用量 - userId: {}, begin: {}, end: {}",
                userId, begin, end);

        List<DailyTokenDTO> data = aiCallLogService.getDailyTokenUsage(
                String.valueOf(userId), begin, end);

        return Result.success(data);
    }


}
