package com.market.order.dao;

import com.market.order.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper {
    /**
     * 新增评价
     */
    @Insert("INSERT INTO reviews (item_id, user_id, rating, comment) " +
            "VALUES (#{itemId}, #{userId}, #{rating}, #{comment})")
    @Options(useGeneratedKeys = true, keyProperty = "reviewId")
    void insert(Review review);
    
    /**
     * 获取评价列表（基本查询，无筛选条件，有分页）
     * 关联订单表，同时获取卖家信息
     */
    @Select("SELECT DISTINCT r.review_id, r.item_id, i.title as item_title, i.item_image_url, " +
            "r.user_id, buyer.username, buyer.user_image_url, " +
            "i.user_id as seller_id, seller.username as seller_name, seller.user_image_url as seller_image_url, " +
            "(SELECT o.order_id FROM order_items oi " +
            " JOIN orders o ON o.order_id = oi.order_id " +
            " WHERE oi.item_id = r.item_id LIMIT 1) as order_id, " +
            "r.rating, r.comment, r.created_at, " +
            "false as is_reported, '' as report_reason " +
            "FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "ORDER BY r.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findReviewListWithPaging(
            @Param("offset") Integer offset, 
            @Param("limit") Integer limit);
    
    /**
     * 按商品ID查询评价列表（有分页）
     */
    @Select("SELECT DISTINCT r.review_id, r.item_id, i.title as item_title, i.item_image_url, " +
            "r.user_id, buyer.username, buyer.user_image_url, " +
            "i.user_id as seller_id, seller.username as seller_name, seller.user_image_url as seller_image_url, " +
            "(SELECT o.order_id FROM order_items oi " +
            " JOIN orders o ON o.order_id = oi.order_id " +
            " WHERE oi.item_id = r.item_id LIMIT 1) as order_id, " +
            "r.rating, r.comment, r.created_at, " +
            "false as is_reported, '' as report_reason " +
            "FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "WHERE r.item_id = #{itemId} " +
            "ORDER BY r.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findReviewListByItemIdWithPaging(
            @Param("itemId") Integer itemId, 
            @Param("offset") Integer offset, 
            @Param("limit") Integer limit);
    
    /**
     * 按商品标题查询评价列表（有分页）
     */
    @Select("SELECT DISTINCT r.review_id, r.item_id, i.title as item_title, i.item_image_url, " +
            "r.user_id, buyer.username, buyer.user_image_url, " +
            "i.user_id as seller_id, seller.username as seller_name, seller.user_image_url as seller_image_url, " +
            "(SELECT o.order_id FROM order_items oi " +
            " JOIN orders o ON o.order_id = oi.order_id " +
            " WHERE oi.item_id = r.item_id LIMIT 1) as order_id, " +
            "r.rating, r.comment, r.created_at, " +
            "false as is_reported, '' as report_reason " +
            "FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "WHERE i.title LIKE CONCAT('%', #{itemTitle}, '%') " +
            "ORDER BY r.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findReviewListByItemTitleWithPaging(
            @Param("itemTitle") String itemTitle, 
            @Param("offset") Integer offset, 
            @Param("limit") Integer limit);
    
    /**
     * 按评价用户名查询评价列表（有分页）
     */
    @Select("SELECT DISTINCT r.review_id, r.item_id, i.title as item_title, i.item_image_url, " +
            "r.user_id, buyer.username, buyer.user_image_url, " +
            "i.user_id as seller_id, seller.username as seller_name, seller.user_image_url as seller_image_url, " +
            "(SELECT o.order_id FROM order_items oi " +
            " JOIN orders o ON o.order_id = oi.order_id " +
            " WHERE oi.item_id = r.item_id LIMIT 1) as order_id, " +
            "r.rating, r.comment, r.created_at, " +
            "false as is_reported, '' as report_reason " +
            "FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "WHERE buyer.username LIKE CONCAT('%', #{username}, '%') " +
            "ORDER BY r.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findReviewListByUsernameWithPaging(
            @Param("username") String username, 
            @Param("offset") Integer offset, 
            @Param("limit") Integer limit);

    /**
     * 按卖家用户名查询评价列表（有分页）
     */
    @Select("SELECT DISTINCT r.review_id, r.item_id, i.title as item_title, i.item_image_url, " +
            "r.user_id, buyer.username, buyer.user_image_url, " +
            "i.user_id as seller_id, seller.username as seller_name, seller.user_image_url as seller_image_url, " +
            "(SELECT o.order_id FROM order_items oi " +
            " JOIN orders o ON o.order_id = oi.order_id " +
            " WHERE oi.item_id = r.item_id LIMIT 1) as order_id, " +
            "r.rating, r.comment, r.created_at, " +
            "false as is_reported, '' as report_reason " +
            "FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "WHERE seller.username LIKE CONCAT('%', #{sellerName}, '%') " +
            "ORDER BY r.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findReviewListBySellerNameWithPaging(
            @Param("sellerName") String sellerName, 
            @Param("offset") Integer offset, 
            @Param("limit") Integer limit);
    
    /**
     * 按卖家ID查询评价列表（有分页）
     */
    @Select("SELECT DISTINCT r.review_id, r.item_id, i.title as item_title, i.item_image_url, " +
            "r.user_id, buyer.username, buyer.user_image_url, " +
            "i.user_id as seller_id, seller.username as seller_name, seller.user_image_url as seller_image_url, " +
            "(SELECT o.order_id FROM order_items oi " +
            " JOIN orders o ON o.order_id = oi.order_id " +
            " WHERE oi.item_id = r.item_id LIMIT 1) as order_id, " +
            "r.rating, r.comment, r.created_at, " +
            "false as is_reported, '' as report_reason " +
            "FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "WHERE i.user_id = #{sellerId} " +
            "ORDER BY r.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findReviewListBySellerIdWithPaging(
            @Param("sellerId") Integer sellerId, 
            @Param("offset") Integer offset, 
            @Param("limit") Integer limit);
    
    /**
     * 按评分范围查询评价列表（有分页）
     */
    @Select("SELECT DISTINCT r.review_id, r.item_id, i.title as item_title, i.item_image_url, " +
            "r.user_id, buyer.username, buyer.user_image_url, " +
            "i.user_id as seller_id, seller.username as seller_name, seller.user_image_url as seller_image_url, " +
            "(SELECT o.order_id FROM order_items oi " +
            " JOIN orders o ON o.order_id = oi.order_id " +
            " WHERE oi.item_id = r.item_id LIMIT 1) as order_id, " +
            "r.rating, r.comment, r.created_at, " +
            "false as is_reported, '' as report_reason " +
            "FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "WHERE r.rating >= #{minRating} AND r.rating <= #{maxRating} " +
            "ORDER BY r.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findReviewListByRatingRangeWithPaging(
            @Param("minRating") Integer minRating, 
            @Param("maxRating") Integer maxRating, 
            @Param("offset") Integer offset, 
            @Param("limit") Integer limit);
    
    /**
     * 统计所有评价总数
     */
    @Select("SELECT COUNT(DISTINCT r.review_id) FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users u ON r.user_id = u.user_id")
    int countAllReviews();
    
    /**
     * 按商品ID统计评价总数
     */
    @Select("SELECT COUNT(DISTINCT r.review_id) FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users u ON r.user_id = u.user_id " +
            "WHERE r.item_id = #{itemId}")
    int countReviewsByItemId(@Param("itemId") Integer itemId);
    
    /**
     * 按商品标题统计评价总数
     */
    @Select("SELECT COUNT(DISTINCT r.review_id) FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users u ON r.user_id = u.user_id " +
            "WHERE i.title LIKE CONCAT('%', #{itemTitle}, '%')")
    int countReviewsByItemTitle(@Param("itemTitle") String itemTitle);
    
    /**
     * 按用户名统计评价总数
     */
    @Select("SELECT COUNT(DISTINCT r.review_id) FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users u ON r.user_id = u.user_id " +
            "WHERE u.username LIKE CONCAT('%', #{username}, '%')")
    int countReviewsByUsername(@Param("username") String username);

    /**
     * 按卖家用户名统计评价总数
     */
    @Select("SELECT COUNT(DISTINCT r.review_id) FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "WHERE seller.username LIKE CONCAT('%', #{sellerName}, '%')")
    int countReviewsBySellerName(@Param("sellerName") String sellerName);
    
    /**
     * 按卖家ID统计评价总数
     */
    @Select("SELECT COUNT(DISTINCT r.review_id) FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "WHERE i.user_id = #{sellerId}")
    int countReviewsBySellerId(@Param("sellerId") Integer sellerId);
    
    /**
     * 按评分范围统计评价总数
     */
    @Select("SELECT COUNT(DISTINCT r.review_id) FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users u ON r.user_id = u.user_id " +
            "WHERE r.rating >= #{minRating} AND r.rating <= #{maxRating}")
    int countReviewsByRatingRange(
            @Param("minRating") Integer minRating, 
            @Param("maxRating") Integer maxRating);
    
    /**
     * 获取评价列表（向下兼容原来的方法）
     */
    default List<Map<String, Object>> findReviewList(Map<String, Object> params) {
        Integer itemId = params.get("itemId") != null ? 
                Integer.parseInt(params.get("itemId").toString()) : null;
        String itemTitle = params.get("itemTitle") != null ? 
                params.get("itemTitle").toString() : null;
        String username = params.get("username") != null ? 
                params.get("username").toString() : null;
        String sellerName = params.get("sellerName") != null ? 
                params.get("sellerName").toString() : null;
        Integer sellerId = params.get("sellerId") != null ? 
                Integer.parseInt(params.get("sellerId").toString()) : null;
        Integer minRating = params.get("minRating") != null ? 
                Integer.parseInt(params.get("minRating").toString()) : null;
        Integer maxRating = params.get("maxRating") != null ? 
                Integer.parseInt(params.get("maxRating").toString()) : null;
        
        Integer offset = params.get("offset") != null ? 
                Integer.parseInt(params.get("offset").toString()) : 0;
        Integer limit = params.get("limit") != null ? 
                Integer.parseInt(params.get("limit").toString()) : 10;
        
        if (itemId != null) {
            return findReviewListByItemIdWithPaging(itemId, offset, limit);
        } else if (itemTitle != null && !itemTitle.isEmpty()) {
            return findReviewListByItemTitleWithPaging(itemTitle, offset, limit);
        } else if (username != null && !username.isEmpty()) {
            return findReviewListByUsernameWithPaging(username, offset, limit);
        } else if (sellerName != null && !sellerName.isEmpty()) {
            return findReviewListBySellerNameWithPaging(sellerName, offset, limit);
        } else if (sellerId != null) {
            return findReviewListBySellerIdWithPaging(sellerId, offset, limit);
        } else if (minRating != null && maxRating != null) {
            return findReviewListByRatingRangeWithPaging(minRating, maxRating, offset, limit);
        } else {
            return findReviewListWithPaging(offset, limit);
        }
    }
    
    /**
     * 统计符合条件的评价总数（向下兼容原来的方法）
     */
    default int countReviews(Map<String, Object> params) {
        Integer itemId = params.get("itemId") != null ? 
                Integer.parseInt(params.get("itemId").toString()) : null;
        String itemTitle = params.get("itemTitle") != null ? 
                params.get("itemTitle").toString() : null;
        String username = params.get("username") != null ? 
                params.get("username").toString() : null;
        String sellerName = params.get("sellerName") != null ? 
                params.get("sellerName").toString() : null;
        Integer sellerId = params.get("sellerId") != null ? 
                Integer.parseInt(params.get("sellerId").toString()) : null;
        Integer minRating = params.get("minRating") != null ? 
                Integer.parseInt(params.get("minRating").toString()) : null;
        Integer maxRating = params.get("maxRating") != null ? 
                Integer.parseInt(params.get("maxRating").toString()) : null;
        
        if (itemId != null) {
            return countReviewsByItemId(itemId);
        } else if (itemTitle != null && !itemTitle.isEmpty()) {
            return countReviewsByItemTitle(itemTitle);
        } else if (username != null && !username.isEmpty()) {
            return countReviewsByUsername(username);
        } else if (sellerName != null && !sellerName.isEmpty()) {
            return countReviewsBySellerName(sellerName);
        } else if (sellerId != null) {
            return countReviewsBySellerId(sellerId);
        } else if (minRating != null && maxRating != null) {
            return countReviewsByRatingRange(minRating, maxRating);
        } else {
            return countAllReviews();
        }
    }
    
    /**
     * 根据ID获取评价详情
     */
    @Select("SELECT r.review_id, r.item_id, i.title as item_title, i.item_image_url, " +
            "r.user_id, buyer.username, buyer.user_image_url, " +
            "i.user_id as seller_id, seller.username as seller_name, seller.user_image_url as seller_image_url, " +
            "(SELECT o.order_id FROM order_items oi " +
            " JOIN orders o ON o.order_id = oi.order_id " +
            " WHERE oi.item_id = r.item_id LIMIT 1) as order_id, " +
            "r.rating, r.comment, r.created_at, " +
            "false as is_reported, '' as report_reason " +
            "FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "JOIN users buyer ON r.user_id = buyer.user_id " +
            "JOIN users seller ON i.user_id = seller.user_id " +
            "WHERE r.review_id = #{reviewId}")
    Map<String, Object> getReviewById(@Param("reviewId") Integer reviewId);
    
    /**
     * 删除评价
     */
    @Delete("DELETE FROM reviews WHERE review_id = #{reviewId}")
    int deleteReview(@Param("reviewId") Integer reviewId);

    /**
     * 批量删除评价
     */
    @Delete("<script>DELETE FROM reviews WHERE review_id IN " +
            "<foreach collection='reviewIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int batchDeleteReviews(@Param("reviewIds") List<Integer> reviewIds);

    /**
     * 获取评分统计
     */
    @Select("SELECT rating, COUNT(DISTINCT r.review_id) as count FROM reviews r GROUP BY rating ORDER BY rating DESC")
    List<Map<String, Object>> getRatingStatistics();
    
    /**
     * 获取评价总数
     */
    @Select("SELECT COUNT(DISTINCT review_id) FROM reviews")
    int getTotalReviews();
    
    /**
     * 获取平均评分
     */
    @Select("SELECT AVG(rating) FROM reviews")
    double getAverageRating();

    /**
     * 获取卖家平均评分
     */
    @Select("SELECT AVG(r.rating) FROM reviews r " +
            "JOIN items i ON r.item_id = i.item_id " +
            "WHERE i.user_id = #{sellerId}")
    Double getAverageRatingBySellerId(@Param("sellerId") Integer sellerId);
    
    /**
     * 检查评价是否存在
     */
    @Select("SELECT COUNT(*) FROM reviews WHERE review_id = #{reviewId}")
    int checkReviewExists(@Param("reviewId") Integer reviewId);

    /**
     * 检查用户是否已评价过该商品
     */
    @Select("SELECT COUNT(*) FROM reviews WHERE item_id = #{itemId} AND user_id = #{userId}")
    int checkUserReviewedItem(@Param("itemId") Integer itemId, @Param("userId") Integer userId);
}
