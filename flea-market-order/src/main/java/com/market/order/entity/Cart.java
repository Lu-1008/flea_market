package com.market.order.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Cart {
    private Integer cartId;
    private Integer userId;
    private LocalDateTime createdAt;
} 