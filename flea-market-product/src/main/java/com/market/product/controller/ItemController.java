package com.market.product.controller;

import com.market.common.ApiResponse;
import com.market.product.entity.Item;
import com.market.product.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/item")
public class ItemController {
    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private ItemService itemService;

    /**
     * 获取商品列表（分页）
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getItemList(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        log.debug("获取商品列表请求 - 参数: title={}, categoryId={}, userId={}, status={}, page={}, size={}",
                title, categoryId, userId, status, page, size);
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("title", title);
            params.put("categoryId", categoryId);
            params.put("userId", userId);
            params.put("status", status);
            params.put("page", page);
            params.put("size", size);
            
            Map<String, Object> result = itemService.getItemList(params);
            log.debug("获取商品列表成功 - 总数: {}, 列表大小: {}", 
                    result.get("total"), ((List)result.get("list")).size());
            return ApiResponse.success("获取商品列表成功", result);
        } catch (Exception e) {
            log.error("获取商品列表失败", e);
            return ApiResponse.error(500, "获取商品列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/{itemId}")
    public ApiResponse<Item> getItemById(@PathVariable Integer itemId) {
        try {
            log.debug("获取商品详情 - ID: {}", itemId);
            
            Item item = itemService.findById(itemId);
            if (item == null) {
                log.warn("商品不存在 - ID: {}", itemId);
                return ApiResponse.error(404, "商品不存在");
            }
            
            return ApiResponse.success("获取商品信息成功", item);
        } catch (Exception e) {
            log.error("获取商品详情失败", e);
            return ApiResponse.error(500, "获取商品详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建商品
     */
    @PostMapping("/create")
    public ApiResponse<Item> createItem(@RequestBody Item item) {
        try {
            log.debug("创建商品请求 - 商品标题: {}", item.getTitle());
            
            // 验证必要字段
            if (item.getTitle() == null || item.getTitle().isEmpty()) {
                return ApiResponse.error(400, "商品标题不能为空");
            }
            
            if (item.getDescription() == null || item.getDescription().isEmpty()) {
                return ApiResponse.error(400, "商品描述不能为空");
            }
            
            if (item.getPrice() == null) {
                return ApiResponse.error(400, "商品价格不能为空");
            }
            
            if (item.getCategoryId() == null) {
                return ApiResponse.error(400, "商品分类不能为空");
            }
            
            // 管理端创建商品时允许userId为null
            // 用户端创建商品时会设置为当前用户ID
            // 如果userId为null，后端服务将设置一个系统默认值
            
            Item createdItem = itemService.createItem(item);
            log.info("商品创建成功 - ID: {}, 标题: {}", createdItem.getItemId(), createdItem.getTitle());
            return ApiResponse.success("创建商品成功", createdItem);
        } catch (Exception e) {
            log.error("创建商品失败", e);
            return ApiResponse.error(500, "创建商品失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品
     */
    @PutMapping("/update")
    public ApiResponse<Item> updateItem(@RequestBody Item item) {
        try {
            log.debug("更新商品请求 - ID: {}, 标题: {}", item.getItemId(), item.getTitle());
            
            // 验证必要字段
            if (item.getItemId() == null) {
                log.warn("更新商品失败 - 商品ID为空");
                return ApiResponse.error(400, "商品ID不能为空");
            }
            
            if (item.getTitle() == null || item.getTitle().isEmpty()) {
                log.warn("更新商品失败 - 商品标题为空");
                return ApiResponse.error(400, "商品标题不能为空");
            }
            
            Item updatedItem = itemService.updateItem(item);
            if (updatedItem == null) {
                log.warn("更新商品失败 - 商品不存在: {}", item.getItemId());
                return ApiResponse.error(404, "商品不存在");
            }
            
            log.info("更新商品成功 - ID: {}, 标题: {}", updatedItem.getItemId(), updatedItem.getTitle());
            return ApiResponse.success("更新商品成功", updatedItem);
        } catch (Exception e) {
            log.error("更新商品失败", e);
            return ApiResponse.error(500, "更新商品失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Integer itemId) {
        try {
            log.debug("删除商品请求 - ID: {}", itemId);
            
            boolean success = itemService.deleteItem(itemId);
            if (!success) {
                log.warn("删除商品失败 - 商品不存在: {}", itemId);
                return ApiResponse.error(404, "商品不存在");
            }
            
            log.info("删除商品成功 - ID: {}", itemId);
            return ApiResponse.success("删除商品成功", null);
        } catch (Exception e) {
            log.error("删除商品失败", e);
            return ApiResponse.error(500, "删除商品失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新商品状态
     */
    @PutMapping("/{itemId}/status")
    public ApiResponse<Item> updateItemStatus(
            @PathVariable Integer itemId,
            @RequestParam String status) {
        
        try {
            log.debug("更新商品状态请求 - ID: {}, 状态: {}", itemId, status);
            
            // 验证状态值
            if (!status.equals("ACTIVE") && !status.equals("SOLD") && !status.equals("INACTIVE")) {
                log.warn("更新商品状态失败 - 无效的状态值: {}", status);
                return ApiResponse.error(400, "无效的状态值，只能是 ACTIVE, SOLD 或 INACTIVE");
            }
            
            Item updatedItem = itemService.updateItemStatus(itemId, status);
            if (updatedItem == null) {
                log.warn("更新商品状态失败 - 商品不存在: {}", itemId);
                return ApiResponse.error(404, "商品不存在");
            }
            
            log.info("更新商品状态成功 - ID: {}, 状态: {}", itemId, status);
            return ApiResponse.success("更新商品状态成功", updatedItem);
        } catch (Exception e) {
            log.error("更新商品状态失败", e);
            return ApiResponse.error(500, "更新商品状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新商品图片URL（专用API，避免触发其他字段验证）
     */
    @PutMapping("/{itemId}/image")
    public ApiResponse<Item> updateItemImage(
            @PathVariable Integer itemId,
            @RequestBody Map<String, String> payload) {
        
        try {
            log.debug("更新商品图片请求 - ID: {}", itemId);
            
            // 获取图片URL
            String itemImageUrl = payload.get("itemImageUrl");
            if (itemImageUrl == null) {
                log.warn("更新商品图片失败 - 图片URL为空");
                return ApiResponse.error(400, "图片URL不能为空");
            }
            
            // 获取当前商品信息
            Item existingItem = itemService.findById(itemId);
            if (existingItem == null) {
                log.warn("更新商品图片失败 - 商品不存在: {}", itemId);
                return ApiResponse.error(404, "商品不存在");
            }
            
            // 只更新图片URL字段
            existingItem.setItemImageUrl(itemImageUrl);
            
            // 调用服务更新商品
            Item updatedItem = itemService.updateItem(existingItem);
            if (updatedItem == null) {
                log.warn("更新商品图片失败 - 更新操作失败: {}", itemId);
                return ApiResponse.error(500, "更新商品图片失败");
            }
            
            log.info("更新商品图片成功 - ID: {}", itemId);
            return ApiResponse.success("更新商品图片成功", updatedItem);
        } catch (Exception e) {
            log.error("更新商品图片失败", e);
            return ApiResponse.error(500, "更新商品图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除商品
     */
    @DeleteMapping("/batch")
    public ApiResponse<Map<String, Object>> batchDeleteItems(@RequestBody List<Integer> itemIds) {
        try {
            log.debug("批量删除商品请求 - 商品IDs: {}", itemIds);
            
            if (itemIds == null || itemIds.isEmpty()) {
                log.warn("批量删除商品失败 - 商品ID列表为空");
                return ApiResponse.error(400, "商品ID列表不能为空");
            }
            
            Map<String, Object> result = new HashMap<>();
            List<Integer> successList = new ArrayList<>();
            List<String> errorList = new ArrayList<>();
            
            for (Integer itemId : itemIds) {
                try {
                    boolean success = itemService.deleteItem(itemId);
                    if (success) {
                        successList.add(itemId);
                    } else {
                        errorList.add("商品ID " + itemId + " 不存在");
                    }
                } catch (Exception e) {
                    errorList.add("商品ID " + itemId + ": 删除失败 - " + e.getMessage());
                }
            }
            
            result.put("success", successList);
            result.put("error", errorList);
            
            log.info("批量删除商品完成 - 成功: {}, 失败: {}", successList.size(), errorList.size());
            
            return ApiResponse.success("批量删除完成", result);
        } catch (Exception e) {
            log.error("批量删除商品失败", e);
            return ApiResponse.error(500, "批量删除商品失败: " + e.getMessage());
        }
    }
} 