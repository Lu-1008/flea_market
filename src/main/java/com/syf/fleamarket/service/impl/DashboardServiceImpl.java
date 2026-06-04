package com.syf.fleamarket.service.impl;

import com.syf.fleamarket.dao.ItemMapper;

import com.syf.fleamarket.dao.UserMapper;
import com.syf.fleamarket.dto.DashboardStatisticsDTO;
import com.syf.fleamarket.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 首页统计数据服务实现类
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ItemMapper itemMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 获取首页统计数据
     * @return 统计数据，包括用户总数、商品总数和总销售额
     */
    @Override
    public DashboardStatisticsDTO getStatistics() {
        int userCount = getUserCount();
        int itemCount = getItemCount();
        double salesAmount = getSalesAmount();
        
        return DashboardStatisticsDTO.builder()
                .userCount(userCount)
                .itemCount(itemCount)
                .salesAmount(salesAmount)
                .build();
    }
    
    /**
     * 获取用户总数
     * @return 用户总数
     */
    @Override
    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    
    /**
     * 获取商品总数
     * @return 商品总数
     */
    @Override
    public int getItemCount() {
        String sql = "SELECT COUNT(*) FROM items";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    
    /**
     * 获取总销售额
     * @return 总销售额
     */
    @Override
    public double getSalesAmount() {
        String sql = "SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM order_items oi " +
                     "JOIN orders o ON oi.order_id = o.order_id " +
                     "WHERE o.status = 'COMPLETED'";
        Double result = jdbcTemplate.queryForObject(sql, Double.class);
        return result != null ? result : 0.0;
    }
} 