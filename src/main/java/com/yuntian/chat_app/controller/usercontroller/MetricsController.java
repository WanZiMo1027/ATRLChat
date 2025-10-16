package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.dto.TokenStatDTO;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.AiCallLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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


}
