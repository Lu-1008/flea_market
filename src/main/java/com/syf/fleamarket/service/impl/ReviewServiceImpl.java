package com.syf.fleamarket.service.impl;

import com.syf.fleamarket.dao.ReviewMapper;
import com.syf.fleamarket.dto.ReviewDTO;
import com.syf.fleamarket.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    @Autowired
    private ReviewMapper reviewMapper;

    @Override
    public Map<String, Object> getReviewList(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 计算分页参数
            int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
            int size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
            int offset = (page - 1) * size;
            
            // 添加分页参数
            params.put("offset", offset);
            params.put("limit", size);
            
            log.debug("获取评价列表，参数: {}", params);
            
            // 查询评价列表
            List<Map<String, Object>> reviews = reviewMapper.findReviewList(params);
            
            // 查询评价总数
            int total = reviewMapper.countReviews(params);
            
            // 构建返回结果
            result.put("list", reviews != null ? reviews : new ArrayList<>());
            result.put("total", total);
        } catch (Exception e) {
            log.error("获取评价列表时出错: {}", e.getMessage(), e);
            result.put("list", new ArrayList<>());
            result.put("total", 0);
        }
        
        return result;
    }

    @Override
    public ReviewDTO getReviewById(Integer reviewId) {
        log.debug("查询评价, ID: {}", reviewId);
        
        try {
            // 先检查评价是否存在
            int exists = reviewMapper.checkReviewExists(reviewId);
            
            if (exists <= 0) {
                log.warn("评价不存在, ID: {}", reviewId);
                return null;
            }
            
            Map<String, Object> reviewMap = reviewMapper.getReviewById(reviewId);
            
            if (reviewMap == null) {
                log.warn("评价不存在, ID: {}", reviewId);
                return null;
            }
            
            // 打印详细的数据信息，用于调试
            log.info("获取到评价详情: {}", reviewMap);
            
            ReviewDTO reviewDTO = convertToReviewDTO(reviewMap);
            
            // 检查关键字段，确保数据完整
            if (reviewDTO != null) {
                log.info("转换后的评价DTO: itemId={}, userId={}, sellerId={}, reviewId={}, comment={}", 
                        reviewDTO.getItemId(), reviewDTO.getUserId(), reviewDTO.getSellerId(), 
                        reviewDTO.getReviewId(), reviewDTO.getComment());
                
                // 如果发现某些关键字段为null，尝试直接从Map中获取
                if (reviewDTO.getItemId() == null && reviewMap.get("item_id") != null) {
                    reviewDTO.setItemId(getIntegerValue(reviewMap.get("item_id"), null));
                    log.info("补充itemId: {}", reviewDTO.getItemId());
                }
                
                if (reviewDTO.getReviewId() == null && reviewMap.get("review_id") != null) {
                    reviewDTO.setReviewId(getIntegerValue(reviewMap.get("review_id"), null));
                    log.info("补充reviewId: {}", reviewDTO.getReviewId());
                }
            } else {
                log.error("评价DTO转换失败，reviewMap={}", reviewMap);
            }
            
            return reviewDTO;
        } catch (Exception e) {
            log.error("查询评价时出错: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将Map转换为ReviewDTO
     */
    private ReviewDTO convertToReviewDTO(Map<String, Object> reviewMap) {
        try {
            if (reviewMap == null) {
                return null;
            }
            
            ReviewDTO reviewDTO = new ReviewDTO();
            
            // 安全地获取各字段值
            reviewDTO.setReviewId(getIntegerValue(reviewMap.get("review_id"), null));
            reviewDTO.setItemId(getIntegerValue(reviewMap.get("item_id"), null));
            reviewDTO.setItemTitle(getStringValue(reviewMap.get("item_title"), ""));
            reviewDTO.setItemImageUrl(getStringValue(reviewMap.get("item_image_url"), ""));
            
            // 评价者信息
            reviewDTO.setUserId(getIntegerValue(reviewMap.get("user_id"), null));
            reviewDTO.setUsername(getStringValue(reviewMap.get("username"), ""));
            reviewDTO.setUserImageUrl(getStringValue(reviewMap.get("user_image_url"), ""));
            
            // 卖家信息
            reviewDTO.setSellerId(getIntegerValue(reviewMap.get("seller_id"), null));
            reviewDTO.setSellerName(getStringValue(reviewMap.get("seller_name"), ""));
            reviewDTO.setSellerImageUrl(getStringValue(reviewMap.get("seller_image_url"), ""));
            
            // 订单信息
            reviewDTO.setOrderId(getIntegerValue(reviewMap.get("order_id"), null));
            
            reviewDTO.setRating(getIntegerValue(reviewMap.get("rating"), 0));
            reviewDTO.setComment(getStringValue(reviewMap.get("comment"), ""));
            
            // 处理created_at时间
            Object createdAtObj = reviewMap.get("created_at");
            if (createdAtObj instanceof java.sql.Timestamp) {
                reviewDTO.setCreatedAt(((java.sql.Timestamp) createdAtObj).toLocalDateTime());
            } else if (createdAtObj instanceof LocalDateTime) {
                reviewDTO.setCreatedAt((LocalDateTime) createdAtObj);
            } else if (createdAtObj instanceof Date) {
                reviewDTO.setCreatedAt(LocalDateTime.ofInstant(((Date) createdAtObj).toInstant(), 
                        java.time.ZoneId.systemDefault()));
            } else {
                reviewDTO.setCreatedAt(LocalDateTime.now());
            }
            
            // 处理is_reported和report_reason
            reviewDTO.setIsReported(getBooleanValue(reviewMap.get("is_reported"), false));
            reviewDTO.setReportReason(getStringValue(reviewMap.get("report_reason"), ""));
            
            return reviewDTO;
        } catch (Exception e) {
            log.error("转换DTO时出错: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 安全获取Integer值
     */
    private Integer getIntegerValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
    
    /**
     * 获取Boolean值，处理不同类型的输入
     */
    private Boolean getBooleanValue(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() > 0;
        } else if (value instanceof String) {
            String strValue = (String) value;
            return "true".equalsIgnoreCase(strValue) || "1".equals(strValue) || "yes".equalsIgnoreCase(strValue);
        }
        
        return defaultValue;
    }
    
    /**
     * 获取String值，处理null情况
     */
    private String getStringValue(Object value, String defaultValue) {
        return value != null ? value.toString() : defaultValue;
    }

    @Override
    @Transactional
    public boolean deleteReview(Integer reviewId) {
        // 检查评价是否存在
        Map<String, Object> existingReview = reviewMapper.getReviewById(reviewId);
        if (existingReview == null) {
            return false;
        }
        
        // 删除评价
        return reviewMapper.deleteReview(reviewId) > 0;
    }

    @Override
    @Transactional
    public Map<String, List<Integer>> batchDeleteReviews(List<Integer> reviewIds) {
        Map<String, List<Integer>> result = new HashMap<>();
        List<Integer> successList = new ArrayList<>();
        List<Integer> errorList = new ArrayList<>();
        
        // 检查每个评价是否存在
        for (Integer reviewId : reviewIds) {
            Map<String, Object> existingReview = reviewMapper.getReviewById(reviewId);
            if (existingReview != null) {
                successList.add(reviewId);
            } else {
                errorList.add(reviewId);
            }
        }
        
        // 批量删除成功的评价
        if (!successList.isEmpty()) {
            reviewMapper.batchDeleteReviews(successList);
        }
        
        result.put("success", successList);
        result.put("error", errorList);
        
        return result;
    }

    @Override
    public Map<String, Object> getReviewStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            // 获取评价总数
            int totalReviews = reviewMapper.getTotalReviews();
            statistics.put("totalReviews", totalReviews);
            
            // 获取平均评分
            if (totalReviews > 0) {
                double averageRating = reviewMapper.getAverageRating();
                statistics.put("averageRating", averageRating);
            } else {
                statistics.put("averageRating", 0.0);
            }
            
            // 获取评分分布
            List<Map<String, Object>> ratingDistribution = reviewMapper.getRatingStatistics();
            statistics.put("ratingDistribution", ratingDistribution != null ? ratingDistribution : new ArrayList<>());
        } catch (Exception e) {
            log.error("获取评价统计信息时出错: {}", e.getMessage(), e);
            statistics.put("totalReviews", 0);
            statistics.put("averageRating", 0.0);
            statistics.put("ratingDistribution", new ArrayList<>());
        }
        
        return statistics;
    }
    
    @Override
    public ReviewMapper getReviewMapper() {
        return reviewMapper;
    }
} 