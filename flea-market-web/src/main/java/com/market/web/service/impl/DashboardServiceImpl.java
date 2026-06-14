package com.market.web.service.impl;

import com.market.order.dao.OrderMapper;
import com.market.product.service.ItemService;
import com.market.user.service.UserService;
import com.market.web.dto.DashboardStatisticsDTO;
import com.market.web.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public DashboardStatisticsDTO getStatistics() {
        return DashboardStatisticsDTO.builder()
                .userCount(getUserCount())
                .itemCount(getItemCount())
                .salesAmount(getSalesAmount())
                .build();
    }

    @Override
    public int getUserCount() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("size", 1);
            Map<String, Object> result = userService.getUserList(params);
            return (Integer) result.get("total");
        } catch (Exception e) {
            log.error("获取用户总数失败", e);
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("size", 1);
            Map<String, Object> result = itemService.getItemList(params);
            return (Integer) result.get("total");
        } catch (Exception e) {
            log.error("获取商品总数失败", e);
            return 0;
        }
    }

    @Override
    public double getSalesAmount() {
        try {
            java.math.BigDecimal totalSales = orderMapper.getTotalSalesAmount();
            return totalSales != null ? totalSales.doubleValue() : 0.0;
        } catch (Exception e) {
            log.error("获取总销售额失败", e);
            return 0.0;
        }
    }
}