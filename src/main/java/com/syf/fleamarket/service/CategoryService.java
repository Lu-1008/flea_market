package com.syf.fleamarket.service;

import com.syf.fleamarket.entity.Category;

import java.util.List;
import java.util.Map;

/**
 * 分类服务接口
 */
public interface CategoryService {
    /**
     * 创建分类
     */
    Category createCategory(Category category);
    
    /**
     * 更新分类
     */
    Category updateCategory(Category category);
    
    /**
     * 删除分类
     */
    boolean deleteCategory(Integer categoryId);
    
    /**
     * 根据ID查询分类
     */
    Category findById(Integer categoryId);
    
    /**
     * 查询所有分类
     */
    List<Category> findAllCategories();
    
    /**
     * 获取分类列表（分页查询）
     */
    Map<String, Object> getCategoryList(Map<String, Object> params);
} 