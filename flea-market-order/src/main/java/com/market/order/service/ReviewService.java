package com.market.order.service;

import com.market.order.dao.ReviewMapper;
import com.market.order.dto.ReviewDTO;

import java.util.List;
import java.util.Map;

/**
 * 评价管理服务接口
 */
public interface ReviewService {
    
    /**
     * 获取评价列表

     */
    Map<String, Object> getReviewList(Map<String, Object> params);
    
    /**
     * 获取评价详情

     */
    ReviewDTO getReviewById(Integer reviewId);
    
    /**
     * 删除评价

     */
    boolean deleteReview(Integer reviewId);
    
    /**
     * 批量删除评价

     */
    Map<String, List<Integer>> batchDeleteReviews(List<Integer> reviewIds);
    
    /**
     * 获取评价统计信息

     */
    Map<String, Object> getReviewStatistics();
    
    /**
     * 获取ReviewMapper

     */
    ReviewMapper getReviewMapper();
} 