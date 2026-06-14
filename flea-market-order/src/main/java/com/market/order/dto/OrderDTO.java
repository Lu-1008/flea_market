package com.market.order.dto;

import com.market.order.entity.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class OrderDTO {
    private Integer orderId;
    private Integer buyerId;
    private String buyerName;
    private String buyerAddress;
    private Integer sellerId;
    private String sellerName;
    private String sellerAddress;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> orderItems;
    private ReviewDTO review;
    private Map<String, Object> contactInfo;
    
    @Data
    public static class OrderItemDTO {
        private Integer orderItemId;
        private Integer orderId;
        private Integer itemId;
        private String itemTitle;
        private BigDecimal price;
        private Integer quantity;
    }
    
    @Data
    public static class ReviewDTO {
        private Integer reviewId;
        private Integer itemId;
        private Integer rating;
        private String comment;
        private LocalDateTime createdAt;
    }
} 