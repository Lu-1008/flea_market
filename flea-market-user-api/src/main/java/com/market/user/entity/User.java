package com.market.user.entity;

import com.market.user.entity.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class User {
    private Integer userId;
    private String username;
    private String password;
    private String email;
    private String phone;
    private UserRole role;
    private LocalDateTime createdAt;
    private String userImgUrl;
    private Double averageRating;
    private String address;
}
