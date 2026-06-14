package com.market.web.controller;

import com.market.common.Result;
import com.market.web.dto.DashboardStatisticsDTO;
import com.market.web.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页统计数据控制器
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取首页统计数据
     * @return 统计数据，包括用户总数、商品总数和总销售额
     */
    @GetMapping("/statistics")
    public Result<DashboardStatisticsDTO> getStatistics() {
        DashboardStatisticsDTO statistics = dashboardService.getStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取用户总数
     * @return 用户总数
     */
    @GetMapping("/user-count")
    public Result<Integer> getUserCount() {
        int userCount = dashboardService.getUserCount();
        return Result.success(userCount);
    }

    /**
     * 获取商品总数
     * @return 商品总数
     */
    @GetMapping("/item-count")
    public Result<Integer> getItemCount() {
        int itemCount = dashboardService.getItemCount();
        return Result.success(itemCount);
    }

    /**
     * 获取总销售额
     * @return 总销售额
     */
    @GetMapping("/sales-amount")
    public Result<Double> getSalesAmount() {
        double salesAmount = dashboardService.getSalesAmount();
        return Result.success(salesAmount);
    }
} 