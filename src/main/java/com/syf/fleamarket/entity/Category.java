package com.syf.fleamarket.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Category {
    private Integer categoryId;
    private String name;
    private Date createdAt;
    private String creator;
} 