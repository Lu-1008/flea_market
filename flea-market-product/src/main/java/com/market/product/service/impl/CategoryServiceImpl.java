package com.market.product.service.impl;

import com.market.product.dao.CategoryMapper;
import com.market.product.dao.ItemMapper;
import com.market.product.entity.Category;
import com.market.product.service.CategoryService;
import com.market.product.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类服务实现类
 */
@Service
public class CategoryServiceImpl implements CategoryService {
    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);
    
    // 默认分类ID，用于在删除分类时转移商品
    private static final Integer DEFAULT_CATEGORY_ID = 1; // 假设ID为1的分类是"服饰鞋帽"
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private ItemMapper itemMapper;
    
    @Autowired
    private ItemService itemService;
    
    @Override
    @Transactional
    public Category createCategory(Category category) {
        log.debug("创建分类 - 名称: {}, 创建人: {}", category.getName(), category.getCreator());
        
        try {
            // 插入分类
            categoryMapper.insert(category);
            log.info("分类创建成功 - ID: {}, 名称: {}", category.getCategoryId(), category.getName());
            
            // 查询完整的分类信息
            return findById(category.getCategoryId());
        } catch (Exception e) {
            log.error("创建分类失败 - 名称: {}", category.getName(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public Category updateCategory(Category category) {
        log.debug("更新分类 - ID: {}, 名称: {}", category.getCategoryId(), category.getName());
        
        try {
            // 检查分类是否存在
            Category existingCategory = categoryMapper.findById(category.getCategoryId());
            if (existingCategory == null) {
                log.warn("更新分类失败 - 分类不存在: {}", category.getCategoryId());
                return null;
            }
            
            // 检查是否有相同名称的分类
            Map<String, Object> params = new HashMap<>();
            params.put("name", category.getName());
            List<Category> existingCategories = categoryMapper.findCategoryList(params);
            
            for (Category existingCat : existingCategories) {
                if (!existingCat.getCategoryId().equals(category.getCategoryId())) {
                    log.warn("更新分类失败 - 已存在相同名称的分类: {}", category.getName());
                    throw new IllegalArgumentException("已存在相同名称的分类");
                }
            }
            
            // 更新分类
            categoryMapper.update(category);
            log.info("分类更新成功 - ID: {}, 名称: {}", category.getCategoryId(), category.getName());
            
            // 查询更新后的完整分类信息
            return findById(category.getCategoryId());
        } catch (IllegalArgumentException e) {
            log.warn("更新分类失败 - 参数错误", e);
            throw e;
        } catch (Exception e) {
            log.error("更新分类失败 - ID: {}", category.getCategoryId(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public boolean deleteCategory(Integer categoryId) {
        log.debug("删除分类 - ID: {}", categoryId);
        
        try {
            // 检查分类是否存在
            Category existingCategory = categoryMapper.findById(categoryId);
            if (existingCategory == null) {
                log.warn("删除分类失败 - 分类不存在: {}", categoryId);
                return false;
            }
            
            // 检查是否为默认分类，不允许删除默认分类
            if (categoryId.equals(DEFAULT_CATEGORY_ID)) {
                log.warn("删除分类失败 - 不允许删除默认分类: {}", categoryId);
                throw new IllegalStateException("不允许删除默认分类");
            }
            
            // 获取该分类下的商品数量
            List<Integer> itemIds = itemMapper.findItemIdsByCategoryId(categoryId);
            int itemCount = itemIds.size();
            
            // 如果有商品，先将它们转移到默认分类
            if (itemCount > 0) {
                log.info("将分类下的{}个商品转移到默认分类(ID: {})", itemCount, DEFAULT_CATEGORY_ID);
                
                int updatedCount = itemService.updateItemCategoryBatch(categoryId, DEFAULT_CATEGORY_ID);
                log.info("成功转移{}个商品到默认分类", updatedCount);
                
                if (updatedCount != itemCount) {
                    log.warn("部分商品转移失败 - 预期: {}, 实际: {}", itemCount, updatedCount);
                }
            }
            
            // 删除分类
            categoryMapper.delete(categoryId);
            log.info("分类删除成功 - ID: {}", categoryId);
            return true;
        } catch (IllegalStateException e) {
            log.warn("删除分类失败 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("删除分类失败 - ID: {}", categoryId, e);
            throw e;
        }
    }
    
    @Override
    public Category findById(Integer categoryId) {
        log.debug("查询分类 - ID: {}", categoryId);
        
        try {
            Category category = categoryMapper.findById(categoryId);
            if (category == null) {
                log.debug("分类不存在 - ID: {}", categoryId);
            }
            return category;
        } catch (Exception e) {
            log.error("查询分类失败 - ID: {}", categoryId, e);
            throw e;
        }
    }
    
    @Override
    public List<Category> findAllCategories() {
        log.debug("查询所有分类");
        
        try {
            List<Category> categories = categoryMapper.findAll();
            log.debug("查询所有分类成功 - 数量: {}", categories.size());
            return categories;
        } catch (Exception e) {
            log.error("查询所有分类失败", e);
            throw e;
        }
    }
    
    @Override
    public Map<String, Object> getCategoryList(Map<String, Object> params) {
        log.debug("获取分类列表 - 参数: {}", params);
        
        try {
            // 处理分页参数
            Integer page = (Integer) params.get("page");
            Integer size = (Integer) params.get("size");
            
            if (page != null && size != null) {
                // 计算偏移量
                int offset = (page - 1) * size;
                params.put("offset", offset);
                params.put("limit", size);
            }
            
            // 处理查询参数
            String categoryName = (String) params.get("categoryName");
            if (categoryName != null && !categoryName.isEmpty()) {
                params.put("name", categoryName);
            }
            
            String creator = (String) params.get("creator");
            if (creator != null && !creator.isEmpty()) {
                params.put("creator", creator);
            }
            
            // 查询分类列表
            List<Category> categories = categoryMapper.findCategoryList(params);
            
            // 查询总数
            int total = categoryMapper.countCategories(params);
            
            // 封装结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", categories);
            result.put("total", total);
            
            log.debug("获取分类列表成功 - 总数: {}, 页码: {}, 每页数量: {}", 
                    total, page, size);
            
            return result;
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            throw e;
        }
    }
} 