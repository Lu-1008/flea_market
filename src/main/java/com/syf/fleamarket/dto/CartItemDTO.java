package com.syf.fleamarket.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Integer cartItemId;
    private Integer itemId;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String itemImageUrl;
    private BigDecimal totalPrice; // 总价 = 单价 * 数量
} 