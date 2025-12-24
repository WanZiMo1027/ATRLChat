package com.yuntian.chat_app.service.adminService;

import com.yuntian.chat_app.vo.DashboardStatsVo;

public interface AdminDashboardService {

    /**
     * 获取仪表盘统计数据
     * @return
     */
    DashboardStatsVo getDashboardStats();
}
