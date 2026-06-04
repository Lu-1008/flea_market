package com.syf.fleamarket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户评价数据传输对象
 */
@Data
public class ReviewDTO {
    private Integer reviewId;
    private Integer itemId;
    private String itemTitle;
    private String itemImageUrl;
    private Integer userId;           // 评价者ID
    private String username;          // 评价者用户名
    private String userImageUrl;      // 评价者头像
    private Integer sellerId;         // 被评价的卖家ID
    private String sellerName;        // 被评价的卖家用户名
    private String sellerImageUrl;    // 被评价的卖家头像
    private Integer orderId;          // 关联的订单ID
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private Boolean isReported;
    private String reportReason;
} 