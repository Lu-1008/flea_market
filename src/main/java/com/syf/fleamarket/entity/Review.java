package com.syf.fleamarket.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Review {
    private Integer reviewId;
    private Integer itemId;
    private Integer userId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
