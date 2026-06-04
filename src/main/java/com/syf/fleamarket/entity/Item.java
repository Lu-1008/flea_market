package com.syf.fleamarket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Item {
    private Integer itemId;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer userId;
    private Integer categoryId;
    private String status; // ACTIVE, SOLD, INACTIVE
    private Date validUntil;
    private Date createdAt;
    private Date updatedAt;
    private String itemImageUrl;
    
    // 额外字段，不在数据库中
    private String categoryName;
    private String username;
}
