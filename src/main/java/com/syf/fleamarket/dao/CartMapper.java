package com.syf.fleamarket.dao;

import com.syf.fleamarket.entity.Cart;
import com.syf.fleamarket.entity.CartItem;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CartMapper {
    
    /**
     * 获取用户的购物车

     */
    Cart getCartByUserId(@Param("userId") Integer userId);
    
    /**
     * 通过购物车ID获取购物车

     */
    Cart getCartById(@Param("cartId") Integer cartId);
    
    /**
     * 获取所有购物车

     */
    List<Cart> getAllCarts();
    
    /**
     * 创建购物车

     */
    int createCart(Cart cart);
    
    /**
     * 获取购物车中的所有商品

     */
    @MapKey("cartItemId")
    List<Map<String, Object>> getCartItems(@Param("cartId") Integer cartId);
    
    /**
     * 获取购物车中特定的商品项

     */
    CartItem getCartItemByItemId(@Param("cartId") Integer cartId, @Param("itemId") Integer itemId);
    
    /**
     * 获取购物车商品项

     */
    CartItem getCartItemById(@Param("cartItemId") Integer cartItemId);
    
    /**
     * 添加商品到购物车

     */
    int addItemToCart(CartItem cartItem);
    
    /**
     * 更新购物车商品数量

     */
    int updateCartItemQuantity(@Param("cartItemId") Integer cartItemId, @Param("quantity") Integer quantity);
    
    /**
     * 从购物车中删除商品

     */
    int removeCartItem(@Param("cartItemId") Integer cartItemId);
    
    /**
     * 清空购物车

     */
    int clearCart(@Param("cartId") Integer cartId);
    
    /**
     * 批量获取购物车商品项
     */
    @MapKey("cartItemId")
    List<Map<String, Object>> getCartItemsByIds(@Param("cartItemIds") List<Integer> cartItemIds);
} 