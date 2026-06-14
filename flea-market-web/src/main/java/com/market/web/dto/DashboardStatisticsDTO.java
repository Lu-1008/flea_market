package com.market.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页统计数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatisticsDTO {
    
    /**
     * 用户总数
     */
    private Integer userCount;
    
    /**
     * 商品总数
     */
    private Integer itemCount;
    
    /**
     * 总销售额
     */
    private Double salesAmount;
} 