package com.yuntian.chat_app.controller.admincontroller;

import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.adminService.AdminDashboardService;
import com.yuntian.chat_app.vo.DashboardStatsVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@Slf4j
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 获取仪表盘统计数据
     * @return
     */
    @GetMapping("/stats")
    public Result<DashboardStatsVo> getStats() {
        log.info("Fetching admin dashboard stats");
        DashboardStatsVo stats = adminDashboardService.getDashboardStats();
        return Result.success(stats);
    }
}
