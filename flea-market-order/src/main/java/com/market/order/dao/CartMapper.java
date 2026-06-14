package com.market.order.dao;

import com.market.order.entity.Cart;
import com.market.order.entity.CartItem;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.MapKey;

import java.util.List;
import java.util.Map;

@Mapper
public interface CartMapper {

    /**
     * 获取用户的购物车
     */
    @Select("SELECT cart_id AS cartId, user_id AS userId, created_at AS createdAt FROM carts WHERE user_id = #{userId} LIMIT 1")
    Cart getCartByUserId(@Param("userId") Integer userId);

    /**
     * 通过购物车ID获取购物车
     */
    @Select("SELECT cart_id AS cartId, user_id AS userId, created_at AS createdAt FROM carts WHERE cart_id = #{cartId} LIMIT 1")
    Cart getCartById(@Param("cartId") Integer cartId);

    /**
     * 获取所有购物车
     */
    @Select("SELECT cart_id AS cartId, user_id AS userId, created_at AS createdAt FROM carts")
    List<Cart> getAllCarts();

    /**
     * 创建购物车
     */
    @Insert("INSERT INTO carts (user_id, created_at) VALUES (#{userId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "cartId")
    int createCart(Cart cart);

    /**
     * 获取购物车中的所有商品
     */
    @MapKey("cartItemId")
    @Select("SELECT ci.cart_item_id AS cartItemId, ci.cart_id AS cartId, ci.item_id AS itemId, ci.quantity, ci.added_at AS addedAt, " +
            "i.title AS itemTitle, i.price, i.item_image_url AS itemImageUrl, i.user_id AS sellerId, " +
            "CASE WHEN i.user_id = -1 THEN '系统商品' ELSE u.username END AS sellerName " +
            "FROM cart_items ci JOIN items i ON ci.item_id = i.item_id " +
            "LEFT JOIN users u ON i.user_id = u.user_id WHERE ci.cart_id = #{cartId} ORDER BY ci.added_at DESC")
    List<Map<String, Object>> getCartItems(@Param("cartId") Integer cartId);

    /**
     * 获取购物车中特定的商品项
     */
    @Select("SELECT cart_item_id AS cartItemId, cart_id AS cartId, item_id AS itemId, quantity, added_at AS addedAt " +
            "FROM cart_items WHERE cart_id = #{cartId} AND item_id = #{itemId} LIMIT 1")
    CartItem getCartItemByItemId(@Param("cartId") Integer cartId, @Param("itemId") Integer itemId);

    /**
     * 获取购物车商品项
     */
    @Select("SELECT cart_item_id AS cartItemId, cart_id AS cartId, item_id AS itemId, quantity, added_at AS addedAt " +
            "FROM cart_items WHERE cart_item_id = #{cartItemId} LIMIT 1")
    CartItem getCartItemById(@Param("cartItemId") Integer cartItemId);

    /**
     * 添加商品到购物车
     */
    @Insert("INSERT INTO cart_items (cart_id, item_id, quantity, added_at) VALUES (#{cartId}, #{itemId}, #{quantity}, #{addedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "cartItemId")
    int addItemToCart(CartItem cartItem);

    /**
     * 更新购物车商品数量
     */
    @Update("UPDATE cart_items SET quantity = #{quantity} WHERE cart_item_id = #{cartItemId}")
    int updateCartItemQuantity(@Param("cartItemId") Integer cartItemId, @Param("quantity") Integer quantity);

    /**
     * 从购物车中删除商品
     */
    @Delete("DELETE FROM cart_items WHERE cart_item_id = #{cartItemId}")
    int removeCartItem(@Param("cartItemId") Integer cartItemId);

    /**
     * 清空购物车
     */
    @Delete("DELETE FROM cart_items WHERE cart_id = #{cartId}")
    int clearCart(@Param("cartId") Integer cartId);

    /**
     * 批量获取购物车商品项
     */
    @MapKey("cartItemId")
    @Select("<script>SELECT ci.cart_item_id AS cartItemId, ci.cart_id AS cartId, ci.item_id AS itemId, ci.quantity, ci.added_at AS addedAt, " +
            "i.title AS itemTitle, i.price, i.item_image_url AS itemImageUrl, i.user_id AS sellerId, " +
            "CASE WHEN i.user_id = -1 THEN '系统商品' ELSE u.username END AS sellerName " +
            "FROM cart_items ci JOIN items i ON ci.item_id = i.item_id " +
            "LEFT JOIN users u ON i.user_id = u.user_id WHERE ci.cart_item_id IN " +
            "<foreach collection='cartItemIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " ORDER BY ci.added_at DESC</script>")
    List<Map<String, Object>> getCartItemsByIds(@Param("cartItemIds") List<Integer> cartItemIds);
}