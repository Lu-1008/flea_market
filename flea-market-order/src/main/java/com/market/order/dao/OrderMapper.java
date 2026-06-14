package com.market.order.dao;

import com.market.order.entity.Order;
import com.market.order.entity.OrderItem;
import com.market.order.entity.OrderStatus;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 获取订单列表（带分页和筛选）
     */
    @Select("<script>SELECT o.order_id AS orderId, o.buyer_id AS buyerId, o.seller_id AS sellerId, " +
            "o.total_amount AS totalAmount, o.status, o.created_at AS createdAt, " +
            "buyer.username AS buyerName, seller.username AS sellerName " +
            "FROM orders o " +
            "LEFT JOIN users buyer ON o.buyer_id = buyer.user_id " +
            "LEFT JOIN users seller ON o.seller_id = seller.user_id " +
            "WHERE 1=1 " +
            "<if test='orderId != null'>AND o.order_id = #{orderId}</if>" +
            "<if test='buyerId != null'>AND o.buyer_id = #{buyerId}</if>" +
            "<if test='buyerName != null'>AND buyer.username LIKE CONCAT('%', #{buyerName}, '%')</if>" +
            "<if test='sellerName != null'>AND seller.username LIKE CONCAT('%', #{sellerName}, '%')</if>" +
            "<if test='status != null and status != \"\"'>AND o.status = #{status}</if>" +
            "ORDER BY o.created_at DESC " +
            "<if test='offset != null and limit != null'>LIMIT #{offset}, #{limit}</if></script>")
    List<Map<String, Object>> getOrderList(Map<String, Object> params);

    /**
     * 获取订单总数（带筛选）
     */
    @Select("<script>SELECT COUNT(*) FROM orders o " +
            "LEFT JOIN users buyer ON o.buyer_id = buyer.user_id " +
            "LEFT JOIN users seller ON o.seller_id = seller.user_id " +
            "WHERE 1=1 " +
            "<if test='orderId != null'>AND o.order_id = #{orderId}</if>" +
            "<if test='buyerId != null'>AND o.buyer_id = #{buyerId}</if>" +
            "<if test='buyerName != null'>AND buyer.username LIKE CONCAT('%', #{buyerName}, '%')</if>" +
            "<if test='sellerName != null'>AND seller.username LIKE CONCAT('%', #{sellerName}, '%')</if>" +
            "<if test='status != null and status != \"\"'>AND o.status = #{status}</if></script>")
    int getOrderCount(Map<String, Object> params);

    /**
     * 根据ID获取订单详情
     */
    @Select("SELECT o.order_id AS orderId, o.buyer_id AS buyerId, o.seller_id AS sellerId, " +
            "o.total_amount AS totalAmount, o.status, o.created_at AS createdAt, " +
            "buyer.username AS buyerName, buyer.email AS buyerEmail, " +
            "seller.username AS sellerName, seller.email AS sellerEmail " +
            "FROM orders o " +
            "LEFT JOIN users buyer ON o.buyer_id = buyer.user_id " +
            "LEFT JOIN users seller ON o.seller_id = seller.user_id " +
            "WHERE o.order_id = #{orderId}")
    Map<String, Object> getOrderById(@Param("orderId") Integer orderId);

    /**
     * 获取订单中的商品列表
     */
    @Select("SELECT oi.order_item_id AS orderItemId, oi.order_id AS orderId, oi.item_id AS itemId, " +
            "oi.price, oi.quantity, i.title AS itemTitle, i.item_image_url AS itemImageUrl " +
            "FROM order_items oi " +
            "JOIN items i ON oi.item_id = i.item_id " +
            "WHERE oi.order_id = #{orderId}")
    List<Map<String, Object>> getOrderItems(@Param("orderId") Integer orderId);

    /**
     * 创建订单
     */
    @Insert("INSERT INTO orders (buyer_id, seller_id, total_amount, status, created_at) " +
            "VALUES (#{buyerId}, #{sellerId}, #{totalAmount}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "orderId")
    int createOrder(Order order);

    /**
     * 批量创建订单商品
     */
    @Insert("<script>INSERT INTO order_items (order_id, item_id, price, quantity) VALUES " +
            "<foreach collection='orderItems' item='item' separator=','>(#{orderId}, #{item.itemId}, #{item.price}, #{item.quantity})</foreach></script>")
    int createOrderItems(@Param("orderId") Integer orderId, @Param("orderItems") List<OrderItem> orderItems);

    /**
     * 更新订单信息
     */
    @Update("UPDATE orders SET buyer_id = #{buyerId}, seller_id = #{sellerId}, " +
            "total_amount = #{totalAmount}, status = #{status} WHERE order_id = #{orderId}")
    int updateOrder(Order order);

    /**
     * 更新订单状态
     */
    @Update("UPDATE orders SET status = #{status} WHERE order_id = #{orderId}")
    int updateOrderStatus(@Param("orderId") Integer orderId, @Param("status") OrderStatus status);

    /**
     * 删除订单
     */
    @Delete("DELETE FROM orders WHERE order_id = #{orderId}")
    int deleteOrder(@Param("orderId") Integer orderId);

    /**
     * 批量删除订单
     */
    @Delete("<script>DELETE FROM orders WHERE order_id IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int batchDeleteOrders(@Param("orderIds") List<Integer> orderIds);

    /**
     * 获取已完成订单的总销售额
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'COMPLETED'")
    java.math.BigDecimal getTotalSalesAmount();

    /**
     * 获取订单的评价信息
     */
    @Select("SELECT r.review_id AS reviewId, r.rating, r.comment, r.created_at AS createdAt, " +
            "r.item_id AS itemId, r.user_id AS userId, u.username " +
            "FROM reviews r " +
            "JOIN users u ON r.user_id = u.user_id " +
            "WHERE r.item_id IN (SELECT item_id FROM order_items WHERE order_id = #{orderId})")
    Map<String, Object> getOrderReview(@Param("orderId") Integer orderId);
}