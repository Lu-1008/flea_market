package com.market.order.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItem {
    private Integer orderItemId;
    private Integer orderId;
    private Integer itemId;
    private BigDecimal price;
    private Integer quantity;
}
