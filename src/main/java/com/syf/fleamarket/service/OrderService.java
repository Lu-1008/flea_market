package com.syf.fleamarket.service;

import com.syf.fleamarket.dto.OrderDTO;
import com.syf.fleamarket.entity.Order;
import com.syf.fleamarket.entity.enums.OrderStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    
    /**
     * 获取订单列表（带分页和筛选）

     */
    Map<String, Object> getOrderList(Map<String, Object> params);
    
    /**
     * 根据ID获取订单详情

     */
    OrderDTO getOrderById(Integer orderId);
    
    /**
     * 创建订单

     */
    OrderDTO createOrder(OrderDTO orderDTO);
    
    /**
     * 更新订单信息

     */
    OrderDTO updateOrder(OrderDTO orderDTO);
    
    /**
     * 更新订单状态

     */
    boolean updateOrderStatus(Integer orderId, OrderStatus status);
    
    /**
     * 删除订单

     */
    boolean deleteOrder(Integer orderId);
    
    /**
     * 批量删除订单

     */
    Map<String, List<Integer>> batchDeleteOrders(List<Integer> orderIds);
} 