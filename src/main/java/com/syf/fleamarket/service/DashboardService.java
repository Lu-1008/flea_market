package com.syf.fleamarket.service;

import com.syf.fleamarket.dto.DashboardStatisticsDTO;

/**
 * 首页统计数据服务接口
 */
public interface DashboardService {
    
    /**
     * 获取首页统计数据

     */
    DashboardStatisticsDTO getStatistics();
    
    /**
     * 获取用户总数

     */
    int getUserCount();
    
    /**
     * 获取商品总数

     */
    int getItemCount();
    
    /**
     * 获取总销售额

     */
    double getSalesAmount();
} 