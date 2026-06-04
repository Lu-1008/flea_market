package com.syf.fleamarket.controller;

import com.syf.fleamarket.common.Result;
import com.syf.fleamarket.dto.ReviewDTO;
import com.syf.fleamarket.entity.Review;
import com.syf.fleamarket.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户评价管理控制器
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 获取评价列表

     * @return 评价列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getReviewList(
            @RequestParam(value = "itemId", required = false) Integer itemId,
            @RequestParam(value = "itemTitle", required = false) String itemTitle,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "sellerName", required = false) String sellerName,
            @RequestParam(value = "sellerId", required = false) Integer sellerId,
            @RequestParam(value = "minRating", required = false) Integer minRating,
            @RequestParam(value = "maxRating", required = false) Integer maxRating,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("itemId", itemId);
            params.put("itemTitle", itemTitle);
            params.put("username", username);
            params.put("sellerName", sellerName);
            params.put("sellerId", sellerId);
            params.put("minRating", minRating);
            params.put("maxRating", maxRating);
            params.put("page", page);
            params.put("size", size);
            
            Map<String, Object> result = reviewService.getReviewList(params);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "获取评价列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取评价详情
     * @param reviewId 评价ID
     * @return 评价详情
     */
    @GetMapping("/{reviewId}")
    public Result<ReviewDTO> getReviewById(@PathVariable Integer reviewId) {
        // 检查参数
        if (reviewId == null || reviewId <= 0) {
            return Result.fail(400, "无效的评价ID");
        }

        try {
            ReviewDTO review = reviewService.getReviewById(reviewId);
            if (review == null) {
                return Result.fail(404, "评价不存在");
            }
            return Result.success(review);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "获取评价详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取卖家评价列表
     * @param sellerId 卖家ID
     * @param page 页码
     * @param size 每页条数
     * @return 卖家评价列表
     */
    @GetMapping("/seller/{sellerId}")
    public Result<Map<String, Object>> getSellerReviews(
            @PathVariable Integer sellerId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("sellerId", sellerId);
            params.put("page", page);
            params.put("size", size);
            
            Map<String, Object> result = reviewService.getReviewList(params);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "获取卖家评价列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除评价
     * @param reviewId 评价ID
     * @return 删除结果
     */
    @DeleteMapping("/{reviewId}")
    public Result<Boolean> deleteReview(@PathVariable Integer reviewId) {
        // 检查参数
        if (reviewId == null || reviewId <= 0) {
            return Result.fail(400, "无效的评价ID");
        }

        try {
            // 尝试获取评价详情，如果不存在则返回404
            ReviewDTO review = reviewService.getReviewById(reviewId);
            if (review == null) {
                return Result.fail(404, "评价不存在");
            }
            
            boolean success = reviewService.deleteReview(reviewId);
            return Result.success(success);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "删除评价失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除评价
     */
    @DeleteMapping("/batch")
    public Result<Map<String, List<Integer>>> batchDeleteReviews(@RequestBody Map<String, List<Integer>> requestBody) {
        List<Integer> reviewIds = requestBody.get("reviewIds");
        if (reviewIds == null || reviewIds.isEmpty()) {
            return Result.fail(400, "缺少评价ID列表");
        }

        try {
            Map<String, List<Integer>> result = reviewService.batchDeleteReviews(reviewIds);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "批量删除评价失败: " + e.getMessage());
        }
    }

    /**
     * 获取评价统计信息
     * @return 评价统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getReviewStatistics() {
        try {
            Map<String, Object> statistics = reviewService.getReviewStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "获取评价统计信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取卖家评分
     * @param sellerId 卖家ID
     * @return 卖家评分
     */
    @GetMapping("/seller/{sellerId}/rating")
    public Result<Double> getSellerRating(@PathVariable Integer sellerId) {
        try {
            Double rating = reviewService.getReviewMapper().getAverageRatingBySellerId(sellerId);
            // 如果没有评价，返回默认值0.0
            return Result.success(rating != null ? rating : 0.0);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "获取卖家评分失败: " + e.getMessage());
        }
    }

    /**
     * 添加评价
     * @param review 评价信息
     * @return 添加结果
     */
    @PostMapping("")
    public Result<Review> createReview(@RequestBody Review review) {
        log.info("接收到评价请求: {}", review);

        // 验证请求参数
        if (review.getItemId() == null || review.getUserId() == null || review.getRating() == null) {
            return Result.fail(400, "缺少必要的参数");
        }

        // 验证评分范围
        if (review.getRating() < 1 || review.getRating() > 5) {
            return Result.fail(400, "评分必须在1-5之间");
        }

        try {
            // 创建时间设置为当前时间
            review.setCreatedAt(LocalDateTime.now());

            // 调用Mapper保存评价
            reviewService.getReviewMapper().insert(review);

            log.info("评价提交成功: {}", review);
            return Result.success(review, "评价提交成功");
        } catch (Exception e) {
            log.error("评价提交失败: {}", e.getMessage(), e);
            return Result.fail(500, "评价提交失败: " + e.getMessage());
        }
    }

}