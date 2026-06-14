package com.market.product.service;

import com.market.product.entity.Item;

import java.util.Map;

/**
 * 商品服务接口
 */
public interface ItemService {
    /**
     * 创建商品

     */
    Item createItem(Item item);
    
    /**
     * 更新商品

     */
    Item updateItem(Item item);
    
    /**
     * 删除商品

     */
    boolean deleteItem(Integer itemId);
    
    /**
     * 根据ID查询商品

     */
    Item findById(Integer itemId);
    
    /**
     * 获取商品列表（支持分页和条件筛选）

     */
    Map<String, Object> getItemList(Map<String, Object> params);
    
    /**
     * 更新商品状态
     */
    Item updateItemStatus(Integer itemId, String status);
    
    /**
     * 批量更新商品分类
     * @param oldCategoryId 原分类ID
     * @param newCategoryId 新分类ID
     * @return 更新的商品数量
     */
    int updateItemCategoryBatch(Integer oldCategoryId, Integer newCategoryId);
} 