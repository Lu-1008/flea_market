package com.syf.fleamarket.controller;

import com.syf.fleamarket.common.ApiResponse;
import com.syf.fleamarket.entity.Category;
import com.syf.fleamarket.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {
    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取分类列表（分页）
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getCategoryList(
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String creator,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        try {
            log.debug("获取分类列表请求 - 参数: categoryName={}, creator={}, page={}, size={}", 
                    categoryName, creator, page, size);
            
            Map<String, Object> params = new HashMap<>();
            params.put("categoryName", categoryName);
            params.put("creator", creator);
            params.put("page", page);
            params.put("size", size);
            
            Map<String, Object> result = categoryService.getCategoryList(params);
            log.debug("获取分类列表成功 - 总数: {}, 列表大小: {}", 
                    result.get("total"), ((List)result.get("list")).size());
            
            return ApiResponse.success("获取分类列表成功", result);
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return ApiResponse.error(500, "获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类详情
     */
    @GetMapping("/{categoryId}")
    public ApiResponse<Category> getCategoryById(@PathVariable Integer categoryId) {
        try {
            log.debug("获取分类详情 - ID: {}", categoryId);
            
            Category category = categoryService.findById(categoryId);
            if (category == null) {
                log.warn("分类不存在 - ID: {}", categoryId);
                return ApiResponse.error(404, "分类不存在");
            }
            
            return ApiResponse.success("获取分类信息成功", category);
        } catch (Exception e) {
            log.error("获取分类详情失败", e);
            return ApiResponse.error(500, "获取分类详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建分类
     */
    @PostMapping("/create")
    public ApiResponse<Category> createCategory(@RequestBody Category category) {
        try {
            log.debug("创建分类请求 - 名称: {}, 创建人: {}", category.getName(), category.getCreator());
            
            // 验证必要字段
            if (category.getName() == null || category.getName().isEmpty()) {
                log.warn("创建分类失败 - 分类名称为空");
                return ApiResponse.error(400, "分类名称不能为空");
            }
            
            Category createdCategory = categoryService.createCategory(category);
            log.info("创建分类成功 - ID: {}, 名称: {}", createdCategory.getCategoryId(), createdCategory.getName());
            
            return ApiResponse.success("创建分类成功", createdCategory);
        } catch (Exception e) {
            log.error("创建分类失败", e);
            return ApiResponse.error(500, "创建分类失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类
     */
    @PutMapping("/update")
    public ApiResponse<Category> updateCategory(@RequestBody Category category) {
        try {
            log.debug("更新分类请求 - ID: {}, 名称: {}, 创建人: {}", 
                    category.getCategoryId(), category.getName(), category.getCreator());
            
            // 验证必要字段
            if (category.getCategoryId() == null) {
                log.warn("更新分类失败 - 分类ID为空");
                return ApiResponse.error(400, "分类ID不能为空");
            }
            
            if (category.getName() == null || category.getName().isEmpty()) {
                log.warn("更新分类失败 - 分类名称为空");
                return ApiResponse.error(400, "分类名称不能为空");
            }
            
            Category updatedCategory = categoryService.updateCategory(category);
            if (updatedCategory == null) {
                log.warn("更新分类失败 - 分类不存在: {}", category.getCategoryId());
                return ApiResponse.error(404, "分类不存在");
            }
            
            log.info("更新分类成功 - ID: {}, 名称: {}, 创建人: {}", 
                    updatedCategory.getCategoryId(), updatedCategory.getName(), updatedCategory.getCreator());
            
            return ApiResponse.success("更新分类成功", updatedCategory);
        } catch (IllegalArgumentException e) {
            log.warn("更新分类失败 - 参数错误: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("更新分类失败", e);
            return ApiResponse.error(500, "更新分类失败: " + e.getMessage());
        }
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable Integer categoryId) {
        try {
            log.debug("删除分类请求 - ID: {}", categoryId);
            
            boolean success = categoryService.deleteCategory(categoryId);
            if (!success) {
                log.warn("删除分类失败 - 分类不存在: {}", categoryId);
                return ApiResponse.error(404, "分类不存在");
            }
            
            log.info("删除分类成功 - ID: {}", categoryId);
            return ApiResponse.success("删除分类成功", null);
        } catch (IllegalStateException e) {
            log.warn("删除分类失败 - 非法状态: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("删除分类失败", e);
            return ApiResponse.error(500, "删除分类失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有分类
     */
    @GetMapping("/all")
    public ApiResponse<List<Category>> getAllCategories() {
        try {
            log.debug("获取所有分类请求");
            
            List<Category> allCategories = categoryService.findAllCategories();
            log.debug("获取所有分类成功 - 数量: {}", allCategories.size());
            
            return ApiResponse.success("获取所有分类列表成功", allCategories);
        } catch (Exception e) {
            log.error("获取所有分类列表失败", e);
            return ApiResponse.error(500, "获取所有分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有分类（用于下拉选择）
     */
    @GetMapping("/categories")
    public ApiResponse<List<Category>> getCategories() {
        try {
            log.debug("获取分类列表请求（下拉选择）");
            
            List<Category> categories = categoryService.findAllCategories();
            log.debug("获取分类列表成功（下拉选择） - 数量: {}", categories.size());
            
            return ApiResponse.success("获取分类列表成功", categories);
        } catch (Exception e) {
            log.error("获取分类列表失败（下拉选择）", e);
            return ApiResponse.error(500, "获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除分类
     */
    @DeleteMapping("/batch")
    public ApiResponse<Map<String, Object>> batchDeleteCategories(@RequestBody List<Integer> categoryIds) {
        try {
            log.debug("批量删除分类请求 - 分类IDs: {}", categoryIds);
            
            if (categoryIds == null || categoryIds.isEmpty()) {
                log.warn("批量删除分类失败 - 分类ID列表为空");
                return ApiResponse.error(400, "分类ID列表不能为空");
            }
            
            Map<String, Object> result = new HashMap<>();
            List<Integer> successList = new ArrayList<>();
            List<String> errorList = new ArrayList<>();
            
            for (Integer categoryId : categoryIds) {
                try {
                    boolean success = categoryService.deleteCategory(categoryId);
                    if (success) {
                        successList.add(categoryId);
                    } else {
                        errorList.add("分类ID " + categoryId + " 不存在");
                    }
                } catch (IllegalStateException e) {
                    errorList.add("分类ID " + categoryId + ": " + e.getMessage());
                } catch (Exception e) {
                    errorList.add("分类ID " + categoryId + ": 删除失败 - " + e.getMessage());
                }
            }
            
            result.put("success", successList);
            result.put("error", errorList);
            
            log.info("批量删除分类完成 - 成功: {}, 失败: {}", successList.size(), errorList.size());
            
            return ApiResponse.success("批量删除完成", result);
        } catch (Exception e) {
            log.error("批量删除分类失败", e);
            return ApiResponse.error(500, "批量删除分类失败: " + e.getMessage());
        }
    }
} 