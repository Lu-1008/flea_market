package com.syf.fleamarket.entity;

import com.syf.fleamarket.entity.enums.UserRole;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
public class User {
    private Integer userId;
    private String username;
    private String password;
    private String email;
    private String phone;
    private UserRole role;
    private LocalDateTime createdAt;
    
    // 用户头像URL，数据库中存储为user_image_url
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String userImgUrl;
    
    // 平均评分在数据库中存储为avg_rating
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Double averageRating;
    
    // 学生宿舍地址
    private String address;
    
    // 获取用户头像URL（如果实际存在）
    public String getUserImgUrl() {
        return userImgUrl != null ? userImgUrl : "user-default.png";
    }
    
    // 设置用户头像URL
    public void setUserImgUrl(String userImgUrl) {
        this.userImgUrl = userImgUrl;
    }
    
    // 获取用户平均评分（如果实际存在）
    public Double getAverageRating() {
        return averageRating != null ? averageRating : 0.0;
    }
    
    // 设置用户平均评分
    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
}
