package com.market.order.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CartItem {
    private Integer cartItemId;
    private Integer cartId;
    private Integer itemId;
    private Integer quantity;
    private LocalDateTime addedAt;
} 