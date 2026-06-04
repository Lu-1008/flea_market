package com.syf.fleamarket.service.impl;

import com.syf.fleamarket.dao.ItemMapper;
import com.syf.fleamarket.entity.Item;
import com.syf.fleamarket.service.ItemService;
import com.syf.fleamarket.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品服务实现类
 */
@Service
public class ItemServiceImpl implements ItemService {
    private static final Logger log = LoggerFactory.getLogger(ItemServiceImpl.class);
    
    private final ItemMapper itemMapper;
    private final FileService fileService;
    
    @Autowired
    public ItemServiceImpl(ItemMapper itemMapper, FileService fileService) {
        this.itemMapper = itemMapper;
        this.fileService = fileService;
        log.info("ItemServiceImpl初始化完成");
    }
    
    @Override
    @Transactional
    public Item createItem(Item item) {
        log.debug("创建商品 - 标题: {}, 价格: {}, 分类: {}", 
                item.getTitle(), item.getPrice(), item.getCategoryId());
        
        // 设置默认状态为ACTIVE
        if (item.getStatus() == null || item.getStatus().isEmpty()) {
            item.setStatus("ACTIVE");
        }
        
        // 插入商品
        itemMapper.insert(item);
        log.info("商品创建成功 - ID: {}, 标题: {}", item.getItemId(), item.getTitle());
        
        // 查询完整的商品信息
        return findById(item.getItemId());
    }
    
    @Override
    @Transactional
    public Item updateItem(Item item) {
        log.debug("更新商品 - ID: {}, 标题: {}", item.getItemId(), item.getTitle());
        
        // 检查商品是否存在
        Item existingItem = itemMapper.findById(item.getItemId());
        if (existingItem == null) {
            log.warn("更新商品失败 - 商品不存在: {}", item.getItemId());
            return null;
        }
        
        // 设置默认状态为ACTIVE，如果状态为空
        if (item.getStatus() == null || item.getStatus().isEmpty()) {
            item.setStatus(existingItem.getStatus() != null ? existingItem.getStatus() : "ACTIVE");
            log.info("商品状态为空，设置为: {}", item.getStatus());
        }
        
        // 更新商品
        itemMapper.update(item);
        log.info("商品更新成功 - ID: {}", item.getItemId());
        
        // 查询更新后的完整商品信息
        return findById(item.getItemId());
    }
    
    @Override
    @Transactional
    public boolean deleteItem(Integer itemId) {
        log.debug("删除商品 - ID: {}", itemId);
        
        // 检查商品是否存在
        Item existingItem = itemMapper.findById(itemId);
        if (existingItem == null) {
            log.warn("删除商品失败 - 商品不存在: {}", itemId);
            return false;
        }
        
        try {
            // 删除商品图片（如果存在）
            if (existingItem.getItemImageUrl() != null && !existingItem.getItemImageUrl().isEmpty()) {
                // 图片在MinIO中的路径格式为item-{itemId}
                String fileKey = "item-" + itemId;
                log.info("删除商品的图片: {}", fileKey);
                
                try {
                    // 调用文件服务删除图片
                    fileService.deleteFile(fileKey);
                    log.info("商品图片删除成功: {}", fileKey);
                } catch (Exception e) {
                    // 即使删除图片失败，也继续删除商品数据
                    log.error("删除商品图片失败: {}, 但仍将继续删除商品", fileKey, e);
                }
            }
            
            // 删除商品
            itemMapper.delete(itemId);
            log.info("商品数据删除成功 - ID: {}", itemId);
            return true;
        } catch (Exception e) {
            log.error("删除商品过程中发生错误: {}", e.getMessage(), e);
            throw e; // 由于@Transactional注解，抛出异常会导致事务回滚
        }
    }
    
    @Override
    public Item findById(Integer itemId) {
        log.debug("查询商品 - ID: {}", itemId);
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            log.debug("商品不存在 - ID: {}", itemId);
        }
        return item;
    }
    
    @Override
    public Map<String, Object> getItemList(Map<String, Object> params) {
        log.debug("获取商品列表 - 参数: {}", params);
        
        // 处理分页参数
        Integer page = (Integer) params.get("page");
        Integer size = (Integer) params.get("size");
        
        if (page != null && size != null) {
            // 计算偏移量
            int offset = (page - 1) * size;
            params.put("offset", offset);
            params.put("limit", size);
        }
        
        try {
            // 查询商品列表
            List<Item> items = itemMapper.findItemList(params);
            
            // 查询总数
            int total = itemMapper.countItems(params);
            
            // 封装结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", items);
            result.put("total", total);
            
            log.debug("获取商品列表成功 - 总数: {}, 页码: {}, 每页数量: {}", 
                    total, page, size);
            
            return result;
        } catch (Exception e) {
            log.error("获取商品列表失败", e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public Item updateItemStatus(Integer itemId, String status) {
        log.debug("更新商品状态 - ID: {}, 状态: {}", itemId, status);
        
        try {
            // 检查商品是否存在
            Item existingItem = itemMapper.findById(itemId);
            if (existingItem == null) {
                log.warn("更新商品状态失败 - 商品不存在: {}", itemId);
                return null;
            }
            
            // 更新商品状态
            itemMapper.updateStatus(itemId, status);
            log.info("商品状态更新成功 - ID: {}, 状态: {}", itemId, status);
            
            // 查询更新后的完整商品信息
            return findById(itemId);
        } catch (Exception e) {
            log.error("更新商品状态失败 - ID: {}", itemId, e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public int updateItemCategoryBatch(Integer oldCategoryId, Integer newCategoryId) {
        log.debug("批量更新商品分类 - 原分类ID: {}, 新分类ID: {}", oldCategoryId, newCategoryId);
        
        try {
            // 检查分类是否相同
            if (oldCategoryId.equals(newCategoryId)) {
                log.info("原分类与新分类相同，无需更新");
                return 0;
            }
            
            // 批量更新商品分类
            int count = itemMapper.updateCategoryBatch(oldCategoryId, newCategoryId);
            log.info("批量更新商品分类成功 - 原分类ID: {}, 新分类ID: {}, 更新数量: {}", oldCategoryId, newCategoryId, count);
            
            return count;
        } catch (Exception e) {
            log.error("批量更新商品分类失败 - 原分类ID: {}, 新分类ID: {}", oldCategoryId, newCategoryId, e);
            throw e;
        }
    }
} 