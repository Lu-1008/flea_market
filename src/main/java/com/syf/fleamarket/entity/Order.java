package com.syf.fleamarket.entity;

import com.syf.fleamarket.entity.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    private Integer orderId;
    private Integer buyerId;
    private Integer sellerId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
