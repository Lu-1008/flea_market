package com.syf.fleamarket.dao;

import com.syf.fleamarket.entity.Order;
import com.syf.fleamarket.entity.OrderItem;
import com.syf.fleamarket.entity.enums.OrderStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    
    /**
     * 获取订单列表（带分页和筛选）

     */
    List<Map<String, Object>> getOrderList(Map<String, Object> params);
    
    /**
     * 获取订单总数（带筛选）

     */
    int getOrderCount(Map<String, Object> params);
    
    /**
     * 根据ID获取订单详情

     */
    Map<String, Object> getOrderById(@Param("orderId") Integer orderId);
    
    /**
     * 获取订单中的商品列表

     */
    List<Map<String, Object>> getOrderItems(@Param("orderId") Integer orderId);
    
    /**
     * 创建订单

     */
    int createOrder(Order order);
    
    /**
     * 批量创建订单商品

     */
    int createOrderItems(List<OrderItem> orderItems);
    
    /**
     * 更新订单信息

     */
    int updateOrder(Order order);
    
    /**
     * 更新订单状态
     */
    int updateOrderStatus(@Param("orderId") Integer orderId, @Param("status") OrderStatus status);
    
    /**
     * 删除订单

     */
    int deleteOrder(@Param("orderId") Integer orderId);
    
    /**
     * 批量删除订单

     */
    int batchDeleteOrders(@Param("orderIds") List<Integer> orderIds);
    
    /**
     * 获取订单的评价信息

     */
    Map<String, Object> getOrderReview(@Param("orderId") Integer orderId);
} 