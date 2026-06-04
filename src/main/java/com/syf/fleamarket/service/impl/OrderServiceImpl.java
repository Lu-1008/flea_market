package com.syf.fleamarket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syf.fleamarket.dao.OrderMapper;
import com.syf.fleamarket.dao.ItemMapper;
import com.syf.fleamarket.dto.OrderDTO;
import com.syf.fleamarket.entity.Order;
import com.syf.fleamarket.entity.OrderItem;
import com.syf.fleamarket.entity.enums.OrderStatus;
import com.syf.fleamarket.entity.enums.ItemStatus;
import com.syf.fleamarket.service.OrderService;
import com.syf.fleamarket.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private ItemMapper itemMapper;
    
    @Autowired
    private ItemService itemService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getOrderList(Map<String, Object> params) {
        // 计算分页参数
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        int offset = (page - 1) * size;
        
        // 添加分页参数
        params.put("offset", offset);
        params.put("limit", size);
        
        // 查询订单列表
        List<Map<String, Object>> orders = orderMapper.getOrderList(params);
        
        // 查询订单总数
        int total = orderMapper.getOrderCount(params);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", orders);
        result.put("total", total);
        
        return result;
    }

    @Override
    public OrderDTO getOrderById(Integer orderId) {
        try {
            // 查询订单基本信息
            Map<String, Object> orderMap = orderMapper.getOrderById(orderId);
            if (orderMap == null || orderMap.isEmpty()) {
                log.debug("未找到ID为{}的订单", orderId);
                return null;
            }
            
            // 减少详细日志，仅在trace级别记录详细信息
            if (log.isTraceEnabled()) {
                log.trace("订单数据: {}", orderMap);
                log.trace("createdAt类型: {}", (orderMap.get("createdAt") != null ? orderMap.get("createdAt").getClass().getName() : "null"));
                log.trace("createdAt值: {}", orderMap.get("createdAt"));
            }
            
            // 转换为DTO
            OrderDTO orderDTO = new OrderDTO();
            try {
                orderDTO.setOrderId(orderMap.get("orderId") != null ? (Integer) orderMap.get("orderId") : orderId);
                orderDTO.setBuyerId(orderMap.get("buyerId") != null ? (Integer) orderMap.get("buyerId") : null);
                orderDTO.setBuyerName(orderMap.get("buyerName") != null ? (String) orderMap.get("buyerName") : "");
                orderDTO.setBuyerAddress(orderMap.get("buyerAddress") != null ? (String) orderMap.get("buyerAddress") : "");
                orderDTO.setSellerId(orderMap.get("sellerId") != null ? (Integer) orderMap.get("sellerId") : null);
                orderDTO.setSellerName(orderMap.get("sellerName") != null ? (String) orderMap.get("sellerName") : "");
                orderDTO.setSellerAddress(orderMap.get("sellerAddress") != null ? (String) orderMap.get("sellerAddress") : "");
                orderDTO.setTotalAmount(orderMap.get("totalAmount") != null ? (java.math.BigDecimal) orderMap.get("totalAmount") : new java.math.BigDecimal("0.00"));
                
                String statusStr = orderMap.get("status") != null ? (String) orderMap.get("status") : "PENDING";
                try {
                    orderDTO.setStatus(OrderStatus.valueOf(statusStr));
                } catch (IllegalArgumentException e) {
                    orderDTO.setStatus(OrderStatus.PENDING);
                }
                
                // 处理创建时间
                Object createdAtObj = orderMap.get("createdAt");
                if (createdAtObj != null) {
                    try {
                        if (createdAtObj instanceof java.sql.Timestamp) {
                            orderDTO.setCreatedAt(((java.sql.Timestamp) createdAtObj).toLocalDateTime());
                        } else if (createdAtObj instanceof String) {
                            // 如果是字符串，尝试解析为LocalDateTime
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            orderDTO.setCreatedAt(LocalDateTime.parse((String) createdAtObj, formatter));
                        } else {
                            // 其他类型尝试转换
                            orderDTO.setCreatedAt(LocalDateTime.now());
                            log.debug("createdAt类型无法识别: {}", createdAtObj.getClass().getName());
                        }
                    } catch (Exception e) {
                        // 如果解析失败，使用当前时间
                        orderDTO.setCreatedAt(LocalDateTime.now());
                        log.debug("解析订单创建时间失败，使用当前时间");
                    }
                } else {
                    // 如果createdAt为null，使用当前时间
                    orderDTO.setCreatedAt(LocalDateTime.now());
                }
                
            } catch (Exception e) {
                // 打印异常信息帮助调试
                log.error("转换订单数据异常", e);
            }
            
            try {
                List<Map<String, Object>> orderItemsMap = orderMapper.getOrderItems(orderId);
                List<OrderDTO.OrderItemDTO> orderItems = new ArrayList<>();
                
                if (orderItemsMap != null && !orderItemsMap.isEmpty()) {
                    orderItems = orderItemsMap.stream().map(item -> {
                        OrderDTO.OrderItemDTO orderItemDTO = new OrderDTO.OrderItemDTO();
                        try {
                            orderItemDTO.setOrderItemId(item.get("orderItemId") != null ? (Integer) item.get("orderItemId") : null);
                            orderItemDTO.setOrderId(item.get("orderId") != null ? (Integer) item.get("orderId") : orderId);
                            orderItemDTO.setItemId(item.get("itemId") != null ? (Integer) item.get("itemId") : null);
                            orderItemDTO.setItemTitle(item.get("itemTitle") != null ? (String) item.get("itemTitle") : "");
                            orderItemDTO.setPrice(item.get("price") != null ? (java.math.BigDecimal) item.get("price") : new java.math.BigDecimal("0.00"));
                            orderItemDTO.setQuantity(item.get("quantity") != null ? (Integer) item.get("quantity") : 0);
                        } catch (Exception e) {
                            log.warn("转换订单商品数据异常: {}", e.getMessage());
                        }
                        return orderItemDTO;
                    }).collect(Collectors.toList());
                }
                
                orderDTO.setOrderItems(orderItems);
            } catch (Exception e) {
                // 如果查询订单商品时出现异常，设置空列表
                orderDTO.setOrderItems(Collections.emptyList());
                log.warn("获取订单商品数据异常: {}", e.getMessage());
            }
            
            // 查询订单评价
            try {
                Map<String, Object> reviewMap = orderMapper.getOrderReview(orderId);
                if (reviewMap != null && !reviewMap.isEmpty()) {
                    OrderDTO.ReviewDTO reviewDTO = new OrderDTO.ReviewDTO();
                    try {
                        reviewDTO.setReviewId(reviewMap.get("reviewId") != null ? (Integer) reviewMap.get("reviewId") : null);
                        reviewDTO.setItemId(reviewMap.get("itemId") != null ? (Integer) reviewMap.get("itemId") : null);
                        reviewDTO.setRating(reviewMap.get("rating") != null ? (Integer) reviewMap.get("rating") : 0);
                        reviewDTO.setComment(reviewMap.get("comment") != null ? (String) reviewMap.get("comment") : "");
                        
                        // 处理评价时间
                        Object reviewCreatedAtObj = reviewMap.get("createdAt");
                        if (reviewCreatedAtObj != null) {
                            try {
                                if (reviewCreatedAtObj instanceof java.sql.Timestamp) {
                                    reviewDTO.setCreatedAt(((java.sql.Timestamp) reviewCreatedAtObj).toLocalDateTime());
                                } else if (reviewCreatedAtObj instanceof String) {
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                    reviewDTO.setCreatedAt(LocalDateTime.parse((String) reviewCreatedAtObj, formatter));
                                } else {
                                    // 其他类型尝试转换
                                    reviewDTO.setCreatedAt(LocalDateTime.now());
                                }
                            } catch (Exception e) {
                                // 如果解析失败，使用当前时间
                                reviewDTO.setCreatedAt(LocalDateTime.now());
                                log.debug("解析评价创建时间失败，使用当前时间");
                            }
                        } else {
                            // 如果时间为null，使用当前时间
                            reviewDTO.setCreatedAt(LocalDateTime.now());
                        }
                    } catch (Exception e) {
                        log.warn("转换评价数据异常: {}", e.getMessage());
                    }
                    
                    orderDTO.setReview(reviewDTO);
                }
            } catch (Exception e) {
                // 如果查询评价时出现异常，忽略评价信息
                log.warn("获取订单评价数据异常: {}", e.getMessage());
            }
            
            return orderDTO;
        } catch (Exception e) {
            // 如果整个查询过程出现异常，返回null
            log.error("获取订单详情失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        // 创建订单实体
        Order order = new Order();
        order.setBuyerId(orderDTO.getBuyerId());
        order.setSellerId(orderDTO.getSellerId());
        order.setTotalAmount(orderDTO.getTotalAmount());
        order.setStatus(OrderStatus.PENDING); // 默认为待处理状态
        
        // 创建订单
        int result = orderMapper.createOrder(order);
        if (result <= 0) {
            return null;
        }
        
        // 批量创建订单商品
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderDTO.OrderItemDTO item : orderDTO.getOrderItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getOrderId());
            orderItem.setItemId(item.getItemId());
            orderItem.setPrice(item.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItems.add(orderItem);
            
            // 更新商品状态为已售出
            itemService.updateItemStatus(item.getItemId(), ItemStatus.SOLD.toString());
        }
        
        orderMapper.createOrderItems(orderItems);
        
        // 返回创建后的订单
        return getOrderById(order.getOrderId());
    }

    @Override
    @Transactional
    public OrderDTO updateOrder(OrderDTO orderDTO) {
        // 检查订单是否存在
        Map<String, Object> existingOrder = orderMapper.getOrderById(orderDTO.getOrderId());
        if (existingOrder == null) {
            return null;
        }
        
        // 创建订单实体
        Order order = new Order();
        order.setOrderId(orderDTO.getOrderId());
        order.setBuyerId(orderDTO.getBuyerId());
        order.setSellerId(orderDTO.getSellerId());
        order.setTotalAmount(orderDTO.getTotalAmount());
        order.setStatus(orderDTO.getStatus());
        
        // 更新订单
        orderMapper.updateOrder(order);
        
        // 返回更新后的订单
        return getOrderById(orderDTO.getOrderId());
    }

    @Override
    @Transactional
    public boolean updateOrderStatus(Integer orderId, OrderStatus status) {
        // 检查订单是否存在
        Map<String, Object> existingOrder = orderMapper.getOrderById(orderId);
        if (existingOrder == null) {
            return false;
        }
        
        // 更新订单状态
        return orderMapper.updateOrderStatus(orderId, status) > 0;
    }

    @Override
    @Transactional
    public boolean deleteOrder(Integer orderId) {
        // 检查订单是否存在
        Map<String, Object> existingOrder = orderMapper.getOrderById(orderId);
        if (existingOrder == null) {
            return false;
        }
        
        // 删除订单
        return orderMapper.deleteOrder(orderId) > 0;
    }

    @Override
    @Transactional
    public Map<String, List<Integer>> batchDeleteOrders(List<Integer> orderIds) {
        Map<String, List<Integer>> result = new HashMap<>();
        List<Integer> successList = new ArrayList<>();
        List<Integer> errorList = new ArrayList<>();
        
        // 检查每个订单是否存在
        for (Integer orderId : orderIds) {
            Map<String, Object> existingOrder = orderMapper.getOrderById(orderId);
            if (existingOrder != null) {
                successList.add(orderId);
            } else {
                errorList.add(orderId);
            }
        }
        
        // 批量删除成功的订单
        if (!successList.isEmpty()) {
            orderMapper.batchDeleteOrders(successList);
        }
        
        result.put("success", successList);
        result.put("error", errorList);
        
        return result;
    }
}