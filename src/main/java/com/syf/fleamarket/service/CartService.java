package com.syf.fleamarket.service;

import com.syf.fleamarket.dto.CartDTO;
import com.syf.fleamarket.dto.OrderDTO;

import java.util.List;

public interface CartService {
    
    /**
     * 获取用户的购物车
     */
    CartDTO getCartByUserId(Integer userId);
    
    /**
     * 向购物车添加商品

     */
    CartDTO addItemToCart(Integer userId, Integer itemId, Integer quantity);
    
    /**
     * 更新购物车中商品的数量

     */
    CartDTO updateCartItemQuantity(Integer cartItemId, Integer quantity);
    
    /**
     * 从购物车中移除商品

     */
    CartDTO removeCartItem(Integer cartItemId);
    
    /**
     * 清空用户的购物车

     */
    boolean clearCart(Integer userId);
    
    /**
     * 购物车结算，创建订单
     */
    List<OrderDTO> checkout(Integer userId, List<Integer> cartItemIds);
} 