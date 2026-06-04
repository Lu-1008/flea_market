package com.syf.fleamarket.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartDTO {
    private Integer cartId;
    private Integer userId;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private List<CartItemDTO> cartItems;
    
    @Data
    public static class CartItemDTO {
        private Integer cartItemId;
        private Integer cartId;
        private Integer itemId;
        private String itemTitle;
        private String itemImage;
        private BigDecimal price;
        private Integer quantity;
        private LocalDateTime addedAt;
        private Integer sellerId;
        private String sellerName;
    }
} 